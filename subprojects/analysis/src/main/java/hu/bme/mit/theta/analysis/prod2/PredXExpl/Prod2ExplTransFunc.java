package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.core.decl.VarDecl;

import java.util.Collection;

public abstract class Prod2ExplTransFunc implements TransFunc<ExplState, StmtAction, ExplPrec> {

	public abstract Collection<VarDecl<?>> getDropouts();

	public abstract void setDropouts(Collection<VarDecl<?>> vars);
}
