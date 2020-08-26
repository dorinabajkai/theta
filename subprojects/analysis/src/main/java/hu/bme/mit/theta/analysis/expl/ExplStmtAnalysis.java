/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.analysis.expl;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.analysis.Analysis;
import hu.bme.mit.theta.analysis.InitFunc;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.analysis.TransFunc;
import hu.bme.mit.theta.analysis.expr.StmtAction;
import hu.bme.mit.theta.analysis.prod2.PredXExpl.Prod2ExplTransFunc;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.solver.Solver;

public final class ExplStmtAnalysis implements Analysis<ExplState, StmtAction, ExplPrec> {

	private final PartialOrd<ExplState> partialOrd;
	private final InitFunc<ExplState, ExplPrec> initFunc;
	private final Prod2ExplTransFunc transFunc;

	private ExplStmtAnalysis(final Solver solver, final Expr<BoolType> initExpr, final int maxSuccToEnumerate) {
		checkNotNull(solver);
		checkNotNull(initExpr);
		this.partialOrd = ExplOrd.getInstance();
		this.initFunc = ExplInitFunc.create(solver, initExpr);
		this.transFunc = ExplStmtTransFunc.create(solver, maxSuccToEnumerate);
	}

	private ExplStmtAnalysis(final Solver solver, final Expr<BoolType> initExpr, final int maxSuccToEnumerate, Prod2ExplTransFunc transFunc) {
		checkNotNull(solver);
		checkNotNull(initExpr);
		this.partialOrd = ExplOrd.getInstance();
		this.initFunc = ExplInitFunc.create(solver, initExpr);
		this.transFunc = transFunc;
	}

	public static ExplStmtAnalysis create(final Solver solver, final Expr<BoolType> initExpr,
										  final int maxSuccToEnumerate) {
		return new ExplStmtAnalysis(solver, initExpr, maxSuccToEnumerate);
	}

	public static ExplStmtAnalysis create(final Solver solver, final Expr<BoolType> initExpr) {
		return create(solver, initExpr, 0);
	}

	public static ExplStmtAnalysis create(final Solver solver, final Expr<BoolType> initExpr, final int maxSuccToEnumerate, Prod2ExplTransFunc transFunc) {
		return new ExplStmtAnalysis(solver, initExpr, maxSuccToEnumerate, transFunc);
	}

	@Override
	public PartialOrd<ExplState> getPartialOrd() {
		return partialOrd;
	}

	@Override
	public InitFunc<ExplState, ExplPrec> getInitFunc() {
		return initFunc;
	}

	@Override
	public Prod2ExplTransFunc getTransFunc() {
		return transFunc;
	}

}
