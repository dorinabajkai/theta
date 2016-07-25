package hu.bme.mit.inf.ttmc.analysis.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Predicate;

import hu.bme.mit.inf.ttmc.analysis.ExprState;
import hu.bme.mit.inf.ttmc.core.expr.Expr;
import hu.bme.mit.inf.ttmc.core.type.BoolType;
import hu.bme.mit.inf.ttmc.formalism.utils.PathUtils;
import hu.bme.mit.inf.ttmc.solver.Solver;

public class ExprStatePredicate implements Predicate<ExprState> {

	private final Expr<? extends BoolType> expr;
	private final Solver solver;

	public ExprStatePredicate(final Expr<? extends BoolType> expr, final Solver solver) {
		this.expr = checkNotNull(expr);
		this.solver = checkNotNull(solver);
	}

	@Override
	public boolean test(final ExprState state) {
		solver.push();
		solver.add(PathUtils.unfold(state.toExpr(), 0));
		solver.add(PathUtils.unfold(expr, 0));
		solver.check();
		final boolean target = solver.getStatus().boolValue();
		solver.pop();
		return target;
	}

}