package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.ExplStateFromState;
import hu.bme.mit.theta.analysis.algorithm.cegar.PrecAdjuster;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
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

public class StatePrecAdjuster<S extends State> implements PrecAdjuster<S, ExprAction, Prod2Prec<PredPrec, ExplPrec>> {
	private int limit;
	private final LTS lts;
	private Solver solver;
	private ExplStateFromState<S> op;

	private StatePrecAdjuster(int limit, final LTS<? super ExplState, ? extends ExprAction> lts, Solver solver, ExplStateFromState<S> op) {
		this.limit = limit;
		this.lts = lts;
		this.solver = solver;
		this.op = op;
	}

	public static <S extends State>StatePrecAdjuster create(final Solver solver, final int limit, final LTS lts, ExplStateFromState<S> op){
		return new StatePrecAdjuster(limit, lts, solver, op);
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> adjust(Prod2Prec<PredPrec, ExplPrec> prec, ArgNode<S, ExprAction> node) {
		checkNotNull(node);
		checkNotNull(prec);
		boolean removed = true;
		Collection<VarDecl<?>> dropouts = prec.getDropouts();

		final ExplState state = op.toExplState(node.getState());
		final Collection<? extends ExprAction> actions = lts.getEnabledActionsFor(node.getState());

		ExplPrec newPrec = prec.getPrec2();

		for (final ExprAction action : actions) {

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
		}

		return Prod2Prec.of(prec.getPrec1(), newPrec, dropouts);
	}

}

