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
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.NullaryExpr;

import java.util.*;

public class PathPrecAdjusterWithT implements PrecAdjuster<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {
	private int limit;

	private PathPrecAdjusterWithT(int limit){
		this.limit = limit;
	}

	public static PathPrecAdjusterWithT create(final int limit){
		return new PathPrecAdjusterWithT(limit);
	}

	@Override
	public CfaPrec<Prod2Prec<PredPrec, ExplPrec>> adjust(CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node) {
		CFA.Loc loc = node.getState().getLoc();

		Set<VarDecl<?>> dropouts = prec.getPrec(loc).getDropouts();

		Map<VarDecl, Collection<NullaryExpr<?>>> counter = new HashMap<>();
		Object[] ancestors = node.ancestors().toArray();
		for(Object a :  ancestors){
			ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> thisnode = (ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction>) a;
			counter = addVars(counter, thisnode);
		}

		counter = addVars(counter, node);

		Collection<VarDecl<?>> vars = new ArrayList<>();
		vars.addAll(prec.getPrec(loc).getPrec2().getVars());
		for (VarDecl var : counter.keySet()) {
			Collection<NullaryExpr<?>> values = counter.get(var);
			if (values.size() > limit) {
				if(!(dropouts.contains(var)))
					dropouts.add(var);
				vars.remove(var);
			}
		}

		return prec.refine(loc, Prod2Prec.of(prec.getPrec(loc).getPrec1(), ExplPrec.of(vars), dropouts));
	}

	public Map<VarDecl, Collection<NullaryExpr<?>>> addVars (Map<VarDecl, Collection<NullaryExpr<?>>> counter, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node){
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
