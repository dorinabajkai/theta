package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;

public interface PrecAdjuster<S extends State, A extends Action, P extends Prec> {
	P adjust(P prec, ArgNode<S ,A> node);
}
