package hu.bme.mit.theta.formalism.cfa;

import hu.bme.mit.theta.formalism.common.Automaton;

public interface CFA extends Automaton<CfaLoc, CfaEdge> {

	public CfaLoc getFinalLoc();

	public CfaLoc getErrorLoc();

}