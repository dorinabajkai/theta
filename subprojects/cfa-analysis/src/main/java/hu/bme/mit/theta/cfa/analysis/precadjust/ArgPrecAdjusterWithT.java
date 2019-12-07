package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.PrecAdjuster;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaPrec;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.NullaryExpr;

import java.util.*;

public class ArgPrecAdjusterWithT implements PrecAdjuster<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {
	private Map<VarDecl, Collection<NullaryExpr<?>>> varValues;
	private int limit;

	private ArgPrecAdjusterWithT(int limit){
		this.limit = limit;
		varValues = new HashMap<>();
	}

	public static ArgPrecAdjusterWithT create(final int limit){
		return new ArgPrecAdjusterWithT(limit);
	}

	@Override
	public CfaPrec<Prod2Prec<PredPrec, ExplPrec>> adjust(CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node) {
		Set<VarDecl<?>> dropouts = prec.getPrec(node.getState().getLoc()).getDropouts();

		varValues = addVars(varValues, node);

		Collection<VarDecl<?>> vars = new ArrayList<>(prec.getPrec(node.getState().getLoc()).getPrec2().getVars());
		for (VarDecl var : varValues.keySet()) {
			Collection<NullaryExpr<?>> values = varValues.get(var);
			if (values.size() > limit) {
				dropouts.add(var);
				vars.remove(var);
			}
		}
		return prec.refine(node.getState().getLoc(), Prod2Prec.of(prec.getPrec(node.getState().getLoc()).getPrec1(), ExplPrec.of(vars), dropouts));
	}

	private Map<VarDecl, Collection<NullaryExpr<?>>> addVars (Map<VarDecl, Collection<NullaryExpr<?>>> counter, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node){
		ExplState state = node.getState().getState().getState2();
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
