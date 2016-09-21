package hu.bme.mit.theta.formalism.common;

public interface Edge<L extends Loc<L, E>, E extends Edge<L, E>> {

	public L getSource();

	public L getTarget();

}