package hu.bme.mit.theta.analysis.prod2.PredXExpl;

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
import hu.bme.mit.theta.core.type.NullaryExpr;

import java.util.*;

public class ArgPrecAdjusterWithT<S extends State> implements PrecAdjuster<S, ExprAction, Prod2Prec<PredPrec, ExplPrec>> {
	private Map<VarDecl, Collection<NullaryExpr<?>>> varValues;
	private int limit;
	private ExplStateFromState<S> op;

	private ArgPrecAdjusterWithT(int limit, ExplStateFromState<S> op){
		this.limit = limit;
		varValues = new HashMap<>();
		this.op = op;
	}

	public static <S extends State>ArgPrecAdjusterWithT create(final int limit, ExplStateFromState<S> op){
		return new ArgPrecAdjusterWithT(limit, op);
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> adjust(Prod2Prec<PredPrec, ExplPrec> prec, ArgNode<S, ExprAction> node) {
		Collection<VarDecl<?>> dropouts = prec.getDropouts();

		varValues = addVars(varValues, node);

		Collection<VarDecl<?>> vars = new ArrayList<>(prec.getPrec2().getVars());
		for (VarDecl var : varValues.keySet()) {
			Collection<NullaryExpr<?>> values = varValues.get(var);
			if (values.size() > limit) {
				if(!(dropouts.contains(var)))
					dropouts.add(var);
				vars.remove(var);
			}
		}
		return Prod2Prec.of(prec.getPrec1(), ExplPrec.of(vars), dropouts);
	}

	public Map<VarDecl, Collection<NullaryExpr<?>>> addVars (Map<VarDecl, Collection<NullaryExpr<?>>> counter, ArgNode<S, ExprAction> node){
		ExplState state = op.toExplState(node.getState());
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
