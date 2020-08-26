package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.PredXExpl.Prod2ExplTransFunc;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
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


public class StateBasedTransFunc extends Prod2ExplTransFunc {

	private final Solver solver;
	private final int limit;
	private Collection<VarDecl<?>> dropouts;
	//private Collection<VarDecl<?>> refined;

	private StateBasedTransFunc(final Solver solver, final int limit) {
		this.solver = checkNotNull(solver);
		this.limit = limit;
		dropouts = new ArrayList<>();
	}

	public static StateBasedTransFunc create(final Solver solver, final int limit) {
		return new StateBasedTransFunc(solver, limit);
	}

	@Override
	public Collection<VarDecl<?>> getDropouts() {
		return dropouts;
	}

	@Override
	public void setDropouts(Collection<VarDecl<?>> vars) {
		dropouts.clear();
		dropouts.addAll(vars);
	}

	//@Override
	public int getLimit() {
		return limit;
	}

	@Override
	public Collection<? extends ExplState> getSuccStates(final ExplState state, final StmtAction action,
														 final ExplPrec prec) {
		checkNotNull(state);
		checkNotNull(action);
		checkNotNull(prec);
		boolean removed = true;

		ExplPrec newPrec = prec;

		Collection<ExplState> result = null;
		while (removed) {
			removed = false;
			Map<VarDecl, Collection<NullaryExpr<?>>> counter = new HashMap<>();
			try (WithPushPop wpp = new WithPushPop(solver)) {
				solver.add(PathUtils.unfold(BoolExprs.And(state.toExpr(), action.toExpr()), 0));

				result = new ArrayList<>();
				while (solver.check().isSat() && !removed) {
					final Valuation model = solver.getModel();
					final Valuation valuation = PathUtils.extractValuation(model, action.nextIndexing());
					final ExplState newState = newPrec.createState(valuation);
					result.add(newState);
					for (VarDecl var : (Collection<? extends VarDecl<?>>) newState.getDecls()) {
						if (counter.containsKey(var)) {
							if (counter.get(var).contains(newState.eval(var).get()))
								continue;
							Collection<NullaryExpr<?>> values = counter.get(var);
							values.add((NullaryExpr<?>) newState.eval(var).get());
							counter.replace(var, values);
						} else {
							Collection<NullaryExpr<?>> val = new ArrayList<>();
							val.add((NullaryExpr<?>) newState.eval(var).get());
							counter.put(var, val);
						}
					}
					Collection<VarDecl<?>> vars = new ArrayList<>();
					vars.addAll(newPrec.getVars());
					for (VarDecl var : counter.keySet()) {
						Collection<NullaryExpr<?>> values = counter.get(var);
						if (values.size() > limit) {
							dropouts.add(var);
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
		//}
		return result.isEmpty() ? Collections.singleton(ExplState.bottom()) : result;
	}

}
