package hu.bme.mit.theta.cfa.analysis.precadjust;

import hu.bme.mit.theta.analysis.algorithm.ArgNode;
import hu.bme.mit.theta.analysis.algorithm.cegar.PrecAdjuster;
import hu.bme.mit.theta.analysis.expl.ExplPrec;
import hu.bme.mit.theta.analysis.expl.ExplState;
import hu.bme.mit.theta.analysis.expr.ExprAction;
import hu.bme.mit.theta.analysis.pred.PredPrec;
import hu.bme.mit.theta.analysis.pred.PredState;
import hu.bme.mit.theta.analysis.prod2.Prod2Prec;
import hu.bme.mit.theta.analysis.prod2.Prod2State;
import hu.bme.mit.theta.cfa.CFA;
import hu.bme.mit.theta.cfa.analysis.CfaAction;
import hu.bme.mit.theta.cfa.analysis.CfaPrec;
import hu.bme.mit.theta.cfa.analysis.CfaState;
import hu.bme.mit.theta.cfa.analysis.lts.CfaLts;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.stmt.AssumeStmt;
import hu.bme.mit.theta.core.type.NullaryExpr;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.core.utils.PathUtils;
import hu.bme.mit.theta.core.utils.StmtUtils;
import hu.bme.mit.theta.solver.Solver;
import hu.bme.mit.theta.solver.utils.WithPushPop;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static hu.bme.mit.theta.core.type.booltype.BoolExprs.Not;

public class DynamicPrecAdjuster implements PrecAdjuster<CfaState<Prod2State<PredState, ExplState>>, CfaAction, CfaPrec<Prod2Prec<PredPrec, ExplPrec>>> {

	private final Solver solver;
	private final int limit;
	private final CfaLts lts;
	private final CFA cfa;
	private Map<VarDecl, Collection<NullaryExpr<?>>> varValues;

	private DynamicPrecAdjuster(final Solver solver, final int limit, final CfaLts lts, final CFA cfa){
		this.solver = solver;
		this.limit = limit;
		this.lts = lts;
		this.cfa = cfa;
		varValues = new HashMap<>();
	}

	public static DynamicPrecAdjuster create(final Solver solver, final int limit, final CfaLts lts, final CFA cfa){
		return new DynamicPrecAdjuster(solver, limit, lts, cfa);
	}

	@Override
	public CfaPrec<Prod2Prec<PredPrec, ExplPrec>> adjust(CfaPrec<Prod2Prec<PredPrec, ExplPrec>> prec, ArgNode<CfaState<Prod2State<PredState, ExplState>>, CfaAction> node) {
		checkNotNull(node);
		checkNotNull(prec);
		boolean removed = true;
		CFA.Loc loc = node.getState().getLoc();
		Collection<VarDecl<?>> dropouts = prec.getPrec(loc).getDropouts();

		final ExplState state = node.getState().getState().getState2();
		final Collection<? extends ExprAction> actions = lts.getEnabledActionsFor(node.getState());

		ExplPrec explPrec = prec.getPrec(loc).getPrec2();
		PredPrec predPrec = prec.getPrec(loc).getPrec1();

		System.out.println(predPrec);
		System.out.println(explPrec);

		for (final ExprAction action : actions) {

			Collection<ExplState> result = null;
			while (removed) {
				removed = false;
				try (WithPushPop wpp = new WithPushPop(solver)) {
					solver.add(PathUtils.unfold(BoolExprs.And(state.toExpr(), action.toExpr()), 0));

					result = new ArrayList<>();
					while (solver.check().isSat() && !removed) {
						final Valuation model = solver.getModel();
						final Valuation valuation = PathUtils.extractValuation(model, action.nextIndexing());
						final ExplState newState = explPrec.createState(valuation);
						result.add(newState);
						for (VarDecl var : (Collection<? extends VarDecl<?>>) newState.getDecls()) {
							if (varValues.containsKey(var)) {
								if (varValues.get(var).contains(newState.eval(var).get()))
									continue;
								Collection<NullaryExpr<?>> values = varValues.get(var);
								values.add((NullaryExpr<?>) newState.eval(var).get());
								varValues.replace(var, values);
							} else {
								Collection<NullaryExpr<?>> val = new ArrayList<>();
								val.add((NullaryExpr<?>) newState.eval(var).get());
								varValues.put(var, val);
							}
						}
						Collection<VarDecl<?>> vars = new ArrayList<>(explPrec.getVars());
						Collection<VarDecl> varValuesKeys = new ArrayList<>(varValues.keySet());
						for (VarDecl var : varValuesKeys) {
							Collection<NullaryExpr<?>> values = varValues.get(var);
							if (values.size() > limit) {
								dropouts.add(var);
								varValues.remove(var);
								vars.remove(var);
								boolean addpred = true;
								while(addpred) {
									for (CFA.Edge edge : cfa.getEdges()) {
										if (edge.getStmt().getClass().equals(AssumeStmt.class)) {
											if (StmtUtils.getVars(edge.getStmt()).contains(var)) {
												AssumeStmt stmt = (AssumeStmt) edge.getStmt();
												predPrec.join(PredPrec.of(stmt.getCond()));
												addpred = false;
											}
										}
									}
								}
								removed = true;
							}
						}
						if (removed) {
							explPrec = ExplPrec.of(vars);
						}

						solver.add(Not(PathUtils.unfold(newState.toExpr(), action.nextIndexing())));
					}

				}


			}
		}

		return prec.refine(loc, Prod2Prec.of(predPrec, explPrec, dropouts));
	}
}
