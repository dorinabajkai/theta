package hu.bme.mit.theta.analysis.prod2.PredXExpl;

import com.google.common.base.Preconditions;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation;
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec;
import hu.bme.mit.theta.analysis.pred.ExprSplitters;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.utils.ExprUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class Prod2RefToPrec implements RefutationToPrec<Prod2Prec<PredPrec, ExplPrec>, ItpRefutation> {
	private final ExprSplitters.ExprSplitter exprSplitter;

	public Prod2RefToPrec(final ExprSplitters.ExprSplitter exprSplitter) {
		this.exprSplitter = checkNotNull(exprSplitter);
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> toPrec(ItpRefutation refutation, int index, Prod2Prec<PredPrec, ExplPrec> prec) {

		final Set<VarDecl<?>> dropouts = prec.getDropouts();

			final Expr<BoolType> expr = refutation.get(index);
			final Collection<Expr<BoolType>> exprs = exprSplitter.apply(expr);
			final Collection<Expr<BoolType>> preds = new ArrayList<>();
			final Collection<VarDecl<?>> vars = new ArrayList<>();
			boolean isPred = false;

			for (final Expr exp : exprs) {
				for (final VarDecl var : ExprUtils.getVars(exp)) {
					if (dropouts.contains(var)) {
						isPred = true;
					}
				}
				if (isPred) {
					preds.add(exp);
				} else {
					vars.addAll(ExprUtils.getVars(exp));
				}
				isPred = false;
			}

			return Prod2Prec.of(PredPrec.of(preds), ExplPrec.of(vars), dropouts);
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> join(Prod2Prec<PredPrec, ExplPrec> prec1, Prod2Prec<PredPrec, ExplPrec> prec2) {
		Preconditions.checkNotNull(prec1);
		Preconditions.checkNotNull(prec2);
		PredPrec pPrec1 = prec1.getPrec1();
		PredPrec pPrec2 = prec2.getPrec1();
		ExplPrec ePrec1 = prec1.getPrec2();
		ExplPrec ePrec2 = prec2.getPrec2();
		Set<VarDecl<?>> dropouts = prec1.getDropouts();
		dropouts.addAll(prec2.getDropouts());
		return Prod2Prec.of(pPrec1.join(pPrec2), ePrec1.join(ePrec2), dropouts);
	}
}
