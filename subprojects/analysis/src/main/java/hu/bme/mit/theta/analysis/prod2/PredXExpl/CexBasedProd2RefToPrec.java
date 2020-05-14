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
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class CexBasedProd2RefToPrec implements RefutationToPrec<Prod2Prec<PredPrec, ExplPrec>, ItpRefutation> {

	private final ExprSplitters.ExprSplitter exprSplitter;
	private Prod2Context context;

	public CexBasedProd2RefToPrec(final ExprSplitters.ExprSplitter exprSplitter, Prod2Context context) {
		this.exprSplitter = checkNotNull(exprSplitter);
		this.context = context;
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> toPrec(ItpRefutation refutation, int index, Prod2Prec<PredPrec, ExplPrec> prec) {

		final Expr<BoolType> expr = refutation.get(index);
		final Collection<Expr<BoolType>> exprs = exprSplitter.apply(expr);

		final Collection<Expr<BoolType>> preds = new ArrayList<>();
		final Collection<VarDecl<?>> vars = new ArrayList<>();

		if(!context.isUsePred()){
			for (final Expr exp : exprs) {
					vars.addAll(ExprUtils.getVars(exp));
			}
		} else {
			for (final Expr exp : exprs) {
				preds.add(exp);
			}
		}

		return Prod2Prec.of(prec.getPrec1().join(PredPrec.of(preds)), prec.getPrec2().join(ExplPrec.of(vars)));
	}

	@Override
	public Prod2Prec<PredPrec, ExplPrec> join(Prod2Prec<PredPrec, ExplPrec> prec1, Prod2Prec<PredPrec, ExplPrec> prec2) {
		Preconditions.checkNotNull(prec1);
		Preconditions.checkNotNull(prec2);
		PredPrec pPrec1 = prec1.getPrec1();
		PredPrec pPrec2 = prec2.getPrec1();
		ExplPrec ePrec1 = prec1.getPrec2();
		ExplPrec ePrec2 = prec2.getPrec2();
		List<VarDecl<?>> dropouts = new ArrayList();
		dropouts.addAll(prec1.getDropouts());
		dropouts.addAll(prec2.getDropouts());
		return Prod2Prec.of(pPrec1.join(pPrec2), ePrec1.join(ePrec2), dropouts);
	}
}


