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
package hu.bme.mit.theta.analysis.algorithm.cegar;

import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.common.Utils;

/**
 * Represents the result of the Abstractor component, that can be either safe or
 * unsafe.
 */
public final class AbstractorResult<P extends Prec> {

	private final boolean safe;
	P prec;

	public AbstractorResult(final boolean safe, P prec) {
		this.safe = safe;
		this.prec = prec;
	}

	public static <P extends Prec> AbstractorResult safe(P prec) {
		return new AbstractorResult(true, prec);
	}

	public static <P extends Prec> AbstractorResult unsafe(P prec) {
		return new AbstractorResult(false, prec);
	}

	public boolean isSafe() {
		return safe;
	}

	public boolean isUnsafe() {
		return !isSafe();
	}

	public Prec getPrec() {
		return prec;
	}

	public void setPrec(P prec) {
		this.prec = prec;
	}

	@Override
	public String toString() {
		return Utils.lispStringBuilder(getClass().getSimpleName()).add(isSafe() ? "Safe" : "Unsafe").toString();
	}
}
