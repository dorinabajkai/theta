package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import hu.bme.mit.theta.analysis.LTS;
import hu.bme.mit.theta.analysis.Prec;
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

public class PathPrecAdjuster<S extends State> implements PrecAdjuster<S, ExprAction, Prod2Prec<PredPrec, ExplPrec>> {

	private final Solver solver;
	private final int limit;
	private final LTS lts;
	private ExplStateFromState<S> op;


	private PathPrecAdjuster(final Solver solver, final int limit, final LTS lts, ExplStateFromState<S> op) {
		this.solver = checkNotNull(solver);
		this.limit = limit;
		this.lts = lts;
		this.op = op;
	}

	public static <S extends State>PathPrecAdjuster create(final Solver solver, final int limit, final LTS lts, ExplStateFromState<S> op){
		return new PathPrecAdjuster(solver, limit, lts, op);
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> adjust(Prod2Prec<PredPrec, ExplPrec> prec, ArgNode<S, ExprAction> node) {
		checkNotNull(node);
		checkNotNull(prec);
		boolean removed = true;
		Collection<VarDecl<?>> dropouts = prec.getDropouts();
		Map<VarDecl, Collection<NullaryExpr<?>>> varValues = new HashMap<>();


		Object[] ancestors = node.ancestors().toArray();
		for(Object a :  ancestors){
			ArgNode<S, ExprAction> thisnode = (ArgNode<S, ExprAction>) a;
			varValues = addVars(varValues, thisnode);
		}

		varValues = addVars(varValues, node);

		final ExplState state = op.toExplState(node.getState());
		final Collection<? extends ExprAction> actions = lts.getEnabledActionsFor(node.getState());

		ExplPrec newPrec = prec.getPrec2();

		for (final ExprAction action : actions) {
			while (removed) {
				removed = false;
				try (WithPushPop wpp = new WithPushPop(solver)) {
					solver.add(PathUtils.unfold(BoolExprs.And(state.toExpr(), action.toExpr()), 0));

					while (solver.check().isSat() && !removed) {
						final Valuation model = solver.getModel();
						final Valuation valuation = PathUtils.extractValuation(model, action.nextIndexing());
						final ExplState newState = newPrec.createState(valuation);
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

		return Prod2Prec.of(prec.getPrec1(), newPrec, dropouts);
	}

	public Map<VarDecl, Collection<NullaryExpr<?>>> addVars (Map<VarDecl, Collection<NullaryExpr<?>>> counter, ArgNode<S, ExprAction> node){
		ExplState state =  op.toExplState(node.getState());
		for ( VarDecl var : (Collection<? extends VarDecl<?>>) state.getDecls()) {
			if (counter.containsKey(var)) {
				if (counter.get(var).contains(state.eval(var).get()))
					continue;
				Collection<NullaryExpr<?>> values = counter.get(var);
				values.add((NullaryExpr<?>) state.eval(var).get());
				counter.replace(var, values);
			} else {
				Collection<NullaryExpr<?>> val = new ArrayList<>();
				val.add((NullaryExpr<?>) state.eval(var).get());
				counter.put(var, val);
			}
		}
		return counter;
	}
}
