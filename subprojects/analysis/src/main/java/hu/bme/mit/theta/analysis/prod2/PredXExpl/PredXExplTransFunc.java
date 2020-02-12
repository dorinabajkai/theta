package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import hu.bme.mit.theta.analysis.State;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.analysis.prod2.StrengtheningOperator;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

public final class PredXExplTransFunc implements TransFunc<Prod2State<PredState, ExplState>, ExprAction, Prod2Prec<PredPrec, ExplPrec>> {

	private final TransFunc<PredState, ExprAction, PredPrec> transFunc1;
	private final TransFunc<ExplState, StmtAction, ExplPrec> transFunc2;
	private final StrengtheningOperator<PredState, ExplState, PredPrec, ExplPrec> strenghteningOperator;

	private PredXExplTransFunc(final TransFunc<PredState, ExprAction, PredPrec> transFunc1, final TransFunc<ExplState, StmtAction, ExplPrec> transFunc2,
						   final StrengtheningOperator<PredState, ExplState, PredPrec, ExplPrec> strenghteningOperator) {
		this.transFunc1 = checkNotNull(transFunc1);
		this.transFunc2 = checkNotNull(transFunc2);
		this.strenghteningOperator = checkNotNull(strenghteningOperator);
	}

	public static PredXExplTransFunc create(
			final TransFunc<PredState, ExprAction, PredPrec> transFunc1, final TransFunc<ExplState, StmtAction, ExplPrec> transFunc2) {
		return create(transFunc1, transFunc2, (states, prec) -> states);
	}

	public static PredXExplTransFunc create(
			final TransFunc<PredState, ExprAction, PredPrec> transFunc1, final TransFunc<ExplState, StmtAction, ExplPrec> transFunc2,
			final StrengtheningOperator<PredState, ExplState, PredPrec, ExplPrec> strenghteningOperator) {
		return new PredXExplTransFunc(transFunc1, transFunc2, strenghteningOperator);
	}

	@Override
	public Collection<Prod2State<PredState, ExplState>> getSuccStates(final Prod2State<PredState, ExplState> state, final ExprAction action,
																	  final Prod2Prec<PredPrec, ExplPrec> prec) {
		checkNotNull(state);
		checkNotNull(action);
		checkNotNull(prec);

		if (state.isBottom()) {
			return singleton(state);
		}

		PredState pState = PredState.of(state.toExpr());
		final Collection<? extends PredState> succStates1 = transFunc1.getSuccStates(pState, action,
				prec.getPrec1());
		final Optional<? extends PredState> optBottom1 = succStates1.stream().filter(State::isBottom).findAny();

		if (optBottom1.isPresent()) {
			final PredState bottom1 = optBottom1.get();
			return singleton(Prod2State.bottom1(bottom1));
		}

		final Collection<? extends ExplState> succStates2 = transFunc2.getSuccStates(state.getState2(), (StmtAction) action,
				prec.getPrec2());
		final Optional<? extends ExplState> optBottom2 = succStates2.stream().filter(State::isBottom).findAny();

		if (optBottom2.isPresent()) {
			final ExplState bottom2 = optBottom2.get();
			return singleton(Prod2State.bottom2(bottom2));
		}

		final Collection<Prod2State<PredState, ExplState>> succStates = Prod2State.cartesian(succStates1, succStates2);

		return strenghteningOperator.strengthen(succStates, prec);
	}

}
