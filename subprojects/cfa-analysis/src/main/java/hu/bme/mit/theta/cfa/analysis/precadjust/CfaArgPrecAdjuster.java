package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.PrecAdjuster;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.PredXExpl.ArgPrecAdjuster;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaPrec;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.cfa.analysis.lts.CfaLts;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.NullaryExpr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.utils.WithPushPop;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;

public class CfaArgPrecAdjuster implements PrecAdjuster<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {

	private final Solver solver;
	private final int limit;
	private final CfaLts lts;
	private Map<VarDecl, Collection<NullaryExpr<?>>> varValues;

	private CfaArgPrecAdjuster(final Solver solver, final int limit, final CfaLts lts){
		this.solver = solver;
		this.limit = limit;
		this.lts = lts;
		varValues = new HashMap<>();
	}

	public static CfaArgPrecAdjuster create(final Solver solver, final int limit, final CfaLts lts){
		return new CfaArgPrecAdjuster(solver, limit, lts);
	}



	public CfaPrec<Prod2Prec<PredPrec, ExplPrec>> adjust(CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node) {
		checkNotNull(node);
		checkNotNull(prec);
		CFA.Loc loc = node.getState().getLoc();
		Collection<VarDecl<?>> dropouts = prec.getPrec(loc).getDropouts();
		final ExplState state = node.getState().getState().getState2();
		final Collection<? extends CfaAction> actions = lts.getEnabledActionsFor(node.getState());

		//ArgPrecAdjuster precAdjuster = ArgPrecAdjuster.create(solver, limit, lts, (CfaState<Prod2State<PredState, ExplState>> cfaState) -> cfaState.getState().getState2());
		//return prec.refine(loc, precAdjuster.adjust(prec.getPrec(loc), node));
		ExplPrec newPrec = prec.getPrec(loc).getPrec2();

		for (final ExprAction action : actions) {
			boolean removed = true;
			Collection<ExplState> result = null;
			while (removed) {
				removed = false;
				try (WithPushPop wpp = new WithPushPop(solver)) {
					solver.add(PathUtils.unfold(BoolExprs.And(state.toExpr(), action.toExpr()), 0));

					result = new ArrayList<>();
					while (solver.check().isSat() && !removed) {
						final Valuation model = solver.getModel();
						final Valuation valuation = PathUtils.extractValuation(model, action.nextIndexing());
						final ExplState newState = newPrec.createState(valuation);
						result.add(newState);
						for (VarDecl var : (Collection<? extends VarDecl<?>>) newState.getDecls()) {
							if (varValues.containsKey(var)) {
								if (varValues.get(var).contains(newState.eval(var).get()))
									continue;
								Collection<NullaryExpr<?>> values = varValues.get(var);
								values.add((NullaryExpr<?>) newState.eval(var).get());
								varValues.replace(var, values);
							} else {
								Collection<NullaryExpr<?>> val = new ArrayList<>();
								val.add((NullaryExpr<?>) newState.eval(var).get());
								varValues.put(var, val);
							}
						}
						Collection<VarDecl<?>> vars = new ArrayList<>(newPrec.getVars());
						Collection<VarDecl> varValuesKeys = new ArrayList<>(varValues.keySet());
						for (VarDecl var : varValuesKeys) {
							Collection<NullaryExpr<?>> values = varValues.get(var);
							if (values.size() > limit) {
								dropouts.add(var);
								varValues.remove(var);
								vars.remove(var);
								removed = true;
							}
						}
						if (removed) {
							newPrec = ExplPrec.of(vars);
						}

						solver.add(Not(PathUtils.unfold(newState.toExpr(), action.nextIndexing())));
					}

				}


			}
		}

		return prec.refine(loc, Prod2Prec.of(prec.getPrec(loc).getPrec1(), newPrec, dropouts));
	}
}

