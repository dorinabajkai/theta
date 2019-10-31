package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.core.decl.VarDecl;

import java.util.Collection;

public class NoOpPrecAdjuster implements PrecAdjuster {

	private Collection<VarDecl<?>> dropouts;

	@Override
	public Prec adjust(Prec prec, ArgNode node) {
		return prec;
	}
}
