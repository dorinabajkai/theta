package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2State;

import static com.google.common.base.Preconditions.checkNotNull;

public final class PredXExplOrd implements PartialOrd<Prod2State<PredState, ExplState>> {

	private final PartialOrd<PredState> partialOrd1;
	private final PartialOrd<ExplState> partialOrd2;
	private final int secondFirst;


	private PredXExplOrd(final PartialOrd<PredState> partialOrd1, final PartialOrd<ExplState> partialOrd2, final int secondFirst) {
		this.partialOrd1 = checkNotNull(partialOrd1);
		this.partialOrd2 = checkNotNull(partialOrd2);
		this.secondFirst = secondFirst;
	}

	public static PredXExplOrd create(final PartialOrd<PredState> partialOrd1,
																			   final PartialOrd<ExplState> partialOrd2,
																			   final int secondFirst) {
		return new PredXExplOrd(partialOrd1, partialOrd2, secondFirst);
	}

	@Override
	public boolean isLeq(final Prod2State<PredState, ExplState> state1, final Prod2State<PredState, ExplState> state2) {
		if (state1.isBottom()) {
			return true;
		} else if (state2.isBottom()) {
			return false;
		} else if (secondFirst == 0) {
			return partialOrd1.isLeq(state1.getState1(), state2.getState1()) && partialOrd2.isLeq(state1.getState2(), state2.getState2());
		} else if (secondFirst == 1) {
			return partialOrd2.isLeq(state1.getState2(), state2.getState2()) && partialOrd1.isLeq(state1.getState1(), state2.getState1());
		} else {
			PredState pState1 = PredState.of(state1.getState1().toExpr(), state1.getState2().toExpr());
			PredState pState2 = PredState.of(state2.getState1().toExpr(), state2.getState2().toExpr());
			return partialOrd1.isLeq(pState1, pState2);
		}
	}
}


