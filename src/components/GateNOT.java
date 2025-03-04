package components;

/**
 * A Primitive Gate that maps every input to its logical not. In this circuit
 * there may be multiple input pins. Each of them is mapped to its logical
 * {@code not} and output at the output pin at the same index, provided that a
 * Branch is connected to that input pin.
 *
 * @author alexm
 */
final class GateNOT extends PrimitiveGate {

	private static final long serialVersionUID = 4L;

	private final ComponentGraphic g;

	/**
	 * Constructs the NOT Gate with the given number of inputs and outputs.
	 *
	 * @param n the number of pairs of pins.
	 */
	GateNOT(int n) {
		super(n, n);
		g = new GateNOTGraphic(this);
	}

	@Override
	public ComponentType type() {
		return ComponentType.GATENOT;
	}

	@Override
	protected void calculateOutput() {
		for (int i = 0; i < inputPins.length; ++i) {
			// for each individual NOT gate check if a Branch is connected
			// and if it is, produce the correct output
			if (checkBranch(i)) {
				boolean res = !inputPins[i].getActive(0);
				outputPins[i].wake_up(res);
			}
		}
	}

	@Override
	public ComponentGraphic getGraphics() {
		return g;
	}
}
