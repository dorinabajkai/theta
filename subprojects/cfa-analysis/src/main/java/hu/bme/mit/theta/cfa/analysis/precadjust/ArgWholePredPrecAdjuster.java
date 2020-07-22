package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.PrecAdjuster;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;

public class ArgWholePredPrecAdjuster implements PrecAdjuster<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {

	private final Solver solver;
	private final int limit;
	private final CfaLts lts;
	private Map<VarDecl, Collection<NullaryExpr<?>>> varValues;
	private final Collection<VarDecl<?>> allVars;
	private boolean removed = false;

	private ArgWholePredPrecAdjuster(final Solver solver, final int limit, final CfaLts lts, final Collection<VarDecl<?>> allVars) {
		this.solver = solver;
		this.limit = limit;
		this.lts = lts;
		varValues = new HashMap<>();
		this.allVars = allVars;
	}

	public static ArgWholePredPrecAdjuster create(final Solver solver, final int limit, final CfaLts lts, final Collection<VarDecl<?>> allVars) {
		return new ArgWholePredPrecAdjuster(solver, limit, lts, allVars);
	}


	public CfaPrec<Prod2Prec<PredPrec, ExplPrec>> adjust(CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node) {
		checkNotNull(node);
		checkNotNull(prec);
		if (!removed) {
			CFA.Loc loc = node.getState().getLoc();
			final ExplState state = node.getState().getState().getState2();
			final Collection<? extends CfaAction> actions = lts.getEnabledActionsFor(node.getState());

			ExplPrec newPrec = prec.getPrec(loc).getPrec2();

			for (final ExprAction action : actions) {
				Collection<ExplState> result = null;
				try (WithPushPop wpp = new WithPushPop(solver)) {
					solver.add(PathUtils.unfold(BoolExprs.And(state.toExpr(), action.toExpr()), 0));

					result = new ArrayList<>();
					while (solver.check().isSat() && !removed) {
						final Valuation model = solver.getModel();
						final Valuation valuation = PathUtils.extractValuation(model, action.nextIndexing());
						final ExplState newState = newPrec.createState(valuation);
						result.add(newState);
						for (VarDecl var : (Collection<? extends VarDecl<?>>) newState.getDecls()) {
							NullaryExpr<?> value = (NullaryExpr<?>) newState.eval(var).get();
							if (varValues.containsKey(var)) {
								if (varValues.get(var).contains(value))
									continue;
								Collection<NullaryExpr<?>> values = varValues.get(var);
								values.add(value);
								varValues.replace(var, values);
								if (varValues.values().size() > limit) {
									removed = true;
									return prec.refine(loc, Prod2Prec.of(prec.getPrec(loc).getPrec1(), ExplPrec.empty(), allVars));
								}
							} else {
								Collection<NullaryExpr<?>> val = new ArrayList<>();
								val.add(value);
								varValues.put(var, val);
							}
						}

						solver.add(Not(PathUtils.unfold(newState.toExpr(), action.nextIndexing())));
					}

				}


			}

		}

		return prec;
	}
}
