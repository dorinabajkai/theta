package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.expl.ExplState;

public interface ExplStateFromState<S extends State> {
	public ExplState toExplState(S state);
}
