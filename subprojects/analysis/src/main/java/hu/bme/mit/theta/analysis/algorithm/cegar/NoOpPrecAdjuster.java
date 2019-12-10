package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.Action;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.algorithm.ArgNode;

public class NoOpPrecAdjuster<S extends State, A extends Action, P extends Prec> implements PrecAdjuster<S, A, P> {

	private NoOpPrecAdjuster(){}

	public static <S extends State, A extends Action, P extends Prec> NoOpPrecAdjuster<S, A, P> create() {
		return new NoOpPrecAdjuster<>();
	}

	@Override
	public P adjust(P prec, ArgNode<S, A> node) {
		return prec;
	}
}
