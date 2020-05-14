package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.algorithm.ARG;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.ArgTrace;
import hu.bme.mit.theta.analysis.algorithm.cegar.Refiner;
import hu.bme.mit.theta.analysis.algorithm.cegar.RefinerResult;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceChecker;
import hu.bme.mit.theta.analysis.expr.refinement.ExprTraceStatus;
import hu.bme.mit.theta.analysis.expr.refinement.PrecRefiner;
import hu.bme.mit.theta.analysis.expr.refinement.Refutation;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.PredXExpl.Prod2Context;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaPrec;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.common.logging.Logger;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class CounterExampleBasedRefiner<R extends Refutation>
		implements Refiner<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {

	private final ExprTraceChecker<R> exprTraceChecker;
	private final PrecRefiner<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>, R> precRefiner;
	private final Logger logger;
	private Map<List<CFA.Edge>, List<CFA.Loc>> cexList;
	private Prod2Context context;

	private CounterExampleBasedRefiner(final ExprTraceChecker<R> exprTraceChecker,
									   final PrecRefiner<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>, R> precRefiner, Prod2Context context, final Logger logger) {
		this.exprTraceChecker = checkNotNull(exprTraceChecker);
		this.precRefiner = checkNotNull(precRefiner);
		this.logger = checkNotNull(logger);
		cexList = new HashMap<>();
		this.context = context;
	}

	public static <R extends Refutation> CounterExampleBasedRefiner<R> create(
			final ExprTraceChecker<R> exprTraceChecker, final PrecRefiner<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>, R> precRefiner,
			Prod2Context context, final Logger logger) {
		return new CounterExampleBasedRefiner<>(exprTraceChecker, precRefiner, context, logger);
	}

	@Override
	public RefinerResult<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> refine(final ARG<CfaState<Prod2State<PredState, ExplState>>, CfaAction> arg, final CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec) {
		checkNotNull(arg);
		checkNotNull(prec);
		assert !arg.isSafe() : "ARG must be unsafe";

		final ArgTrace<CfaState<Prod2State<PredState, ExplState>>, CfaAction> cexToConcretize = arg.getCexs().findFirst().get();
		final Trace<CfaState<Prod2State<PredState, ExplState>>, CfaAction> traceToConcretize = cexToConcretize.toTrace();
		logger.write(Logger.Level.INFO, "|  |  Trace length: %d%n", traceToConcretize.length());
		logger.write(Logger.Level.DETAIL, "|  |  Trace: %s%n", traceToConcretize);

		logger.write(Logger.Level.SUBSTEP, "|  |  Checking trace...");
		final ExprTraceStatus<R> cexStatus = exprTraceChecker.check(traceToConcretize);
		logger.write(Logger.Level.SUBSTEP, "done, result: %s%n", cexStatus);

		assert cexStatus.isFeasible() || cexStatus.isInfeasible() : "Unknown CEX status";

		if (cexStatus.isFeasible()) {
			return RefinerResult.unsafe(traceToConcretize);
		} else {
			final R refutation = cexStatus.asInfeasible().getRefutation();
			logger.write(Logger.Level.DETAIL, "|  |  |  Refutation: %s%n", refutation);

			List<CFA.Loc> cexLocs = new ArrayList<>();
			List<CFA.Edge> cexEdges = new ArrayList<>();

			for( CfaState state : traceToConcretize.getStates()){
				cexLocs.add(state.getLoc());
			}

			for( CfaAction action : traceToConcretize.getActions()){
				cexEdges.addAll(action.getEdges());
			}

			for(List<CFA.Edge> edgeList : cexList.keySet()){
				if(edgeList.equals(cexEdges) && cexList.get(edgeList).equals(cexLocs))
					context.setUsePred(true);
			}

			cexList.put(cexEdges, cexLocs);

			final CfaPrec<Prod2Prec<PredPrec, ExplPrec>> refinedPrec = precRefiner.refine(prec, traceToConcretize, refutation);
			final int pruneIndex = refutation.getPruneIndex();
			assert 0 <= pruneIndex : "Pruning index must be non-negative";
			assert pruneIndex <= cexToConcretize.length() : "Pruning index larger than cex length";

			logger.write(Logger.Level.SUBSTEP, "|  |  Pruning from index %d...", pruneIndex);
			final ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> nodeToPrune = cexToConcretize.node(pruneIndex);
			arg.prune(nodeToPrune);
			logger.write(Logger.Level.SUBSTEP, "done%n");

			return RefinerResult.spurious(refinedPrec);
		}
	}

	@Override
	public String toString() {
		return Utils.lispStringBuilder(getClass().getSimpleName()).add(exprTraceChecker).add(precRefiner).toString();
	}

}

