package hu.bme.mit.theta.analysis.expr.refinement;

import com.google.common.collect.ImmutableList;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.analysis.expr.ExprStates;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.MutableValuation;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.utils.ExprUtils;
import hu.bme.mit.theta.core.utils.IndexedVars;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.VarIndexing;
import hu.bme.mit.theta.solver.ItpSolver;
import hu.bme.mit.theta.solver.utils.WithPushPop;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;


public class ExprTraceValueItpChecker implements ExprTraceChecker<VarsRefutation> {

	private final ItpSolver solver;
	private final Collection<VarDecl<?>> allVars;

	private ExprTraceValueItpChecker(final ItpSolver solver, final Collection<VarDecl<?>> allVars) {
		this.solver = solver;
		this.allVars = allVars;
	}

	public static ExprTraceValueItpChecker create(final ItpSolver solver, final Collection<VarDecl<?>> allVars) {
		return new ExprTraceValueItpChecker(solver, allVars);
	}

	@Override
	public ExprTraceStatus<VarsRefutation> check(Trace<? extends ExprState, ? extends ExprAction> trace) {
		checkNotNull(trace);
		//trace --> ExplState x CfaAction
		Trace<ExprState, StmtAction> castedTrace = (Trace<ExprState, StmtAction>) trace;
		Collection<ExprState> allStates = castedTrace.getStates();

		boolean concretizable = false;
		final List<VarIndexing> indexings = new ArrayList<>(allStates.size());

		try (WithPushPop wpp = new WithPushPop(solver)) {

			indexings.add(VarIndexing.all(0));
			solver.add(ExprUtils.getConjuncts(PathUtils.unfold(trace.getState(0).toExpr(), indexings.get(0))));

			for (int i = 1; i < allStates.size(); ++i) {
				indexings.add(indexings.get(i - 1).add(trace.getAction(i - 1).nextIndexing()));
				solver.add(ExprUtils.getConjuncts(PathUtils.unfold(trace.getState(i).toExpr(), indexings.get(i))));
				solver.add(ExprUtils
						.getConjuncts(PathUtils.unfold(trace.getAction(i - 1).toExpr(), indexings.get(i - 1))));
			}

			solver.add(ExprUtils.getConjuncts(PathUtils.unfold(BoolExprs.True(), indexings.get(allStates.size() - 1))));
			concretizable = solver.check().isSat();


			if (concretizable) {
				final Valuation model = solver.getModel();
				final ImmutableList.Builder<Valuation> builder = ImmutableList.builder();
				for (final VarIndexing indexing : indexings) {
					builder.add(PathUtils.extractValuation(model, indexing));
				}
				return ExprTraceStatus.feasible(Trace.of(builder.build(), castedTrace.getActions()));
			}
		}

		//initial prec, indexedvars and v
		ExplPrec prec = ExplPrec.of(allVars);
		ExplState v = ExplState.top();
		final IndexedVars.Builder builder = IndexedVars.builder();


		//Cycle for the decreasing gammaPos
		for (int i = 0; i < castedTrace.getActions().size(); i++) {
			//Getting v
			Collection<ExplState> gammaNeg = ExprStates.createStatesForExpr(solver,
					BoolExprs.And(v.toExpr(), castedTrace.getAction(i).toExpr()), 0, prec::createState, castedTrace.getAction(i).nextIndexing(), 1);
			for (ExplState s : gammaNeg) {
				v = s;
			}

			Set<VarDecl<?>> dropouts = new HashSet<>();
			//Removing different vars from v
			for (VarDecl<?> x : (Collection<VarDecl<?>>) v.getDecls()) {
				MutableValuation val = MutableValuation.copyOf(v.getVal());
				val.remove(x);
				ExplState nextState = ExplState.of(val);
				Collection<VarDecl<?>> vars = new ArrayList<>(prec.getVars());
				vars.remove(x);
				//new prec with removed x
				ExplPrec newPrec = ExplPrec.of(vars);

				//Searching for contradiction
				for (int j = i + 1; j < castedTrace.getActions().size(); j++) {
					Collection<ExplState> gammaPos = ExprStates.createStatesForExpr(solver,
							BoolExprs.And(nextState.toExpr(), castedTrace.getAction(j).toExpr()), 0, newPrec::createState, castedTrace.getAction(j).nextIndexing(), 1);
					if (gammaPos.stream().filter(State::isBottom).findAny().isPresent() || gammaPos.isEmpty()) {
						dropouts.add(x);
						break;
					} else {
						for (ExplState s : gammaPos) {
							nextState = s;
						}
					}
				}
			}

			//Removing not contradicting vars from v
			Set<VarDecl<?>> itp = new HashSet<>((Collection<? extends VarDecl<?>>) v.getDecls());
			for (VarDecl<?> variable : dropouts)
				itp.remove(variable);

			//Setting itp to current state
			builder.add(i, itp);

		}

		final IndexedVars indexedVars = builder.build();
		return ExprTraceStatus.infeasible(VarsRefutation.create(indexedVars));


	}
}


