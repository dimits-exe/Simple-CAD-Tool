package command;

import static components.ComponentType.BRANCH;
import static requirement.StringType.ANY;
import static requirement.StringType.CUSTOM;
import static requirement.StringType.NON_NEG_INTEGER;
import static requirement.StringType.POS_INTEGER;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import application.editor.Editor;
import application.editor.CycleException;
import application.editor.MissingComponentException;
import components.Component;
import components.ComponentFactory;
import components.ComponentType;
import exceptions.MalformedBranchException;
import myUtil.Utility;
import requirement.Requirements;

/**
 * A Command that creates a basic {@code Component} and subsequently adds it to
 * the {@code context}.
 *
 * @author alexm
 */
class CreateCommand extends Command {

	private static final long serialVersionUID = 6L;

	private final ComponentType componentType;

	private final List<Command> deleteCommands;

	/**
	 * Creates the Command initialising its {@code requirements}.
	 *
	 * @param editor the {@code context} of this Command
	 * @param type   the type of Components this Command creates
	 */
	CreateCommand(Editor editor, ComponentType type) {
		super(editor);
		componentType = type;
		deleteCommands = new ArrayList<>();

		switch (componentType) {
		case BRANCH:
			requirements.add("in id", ANY);
			requirements.add("in index", NON_NEG_INTEGER);
			requirements.add("out id", ANY);
			requirements.add("out index", NON_NEG_INTEGER);
			break;
		case GATEAND:
		case GATENOT:
		case GATEOR:
		case GATEXOR:
			requirements.add("in count", POS_INTEGER);
			break;
		case INPUT_PIN:
		case OUTPUT_PIN:
		default:
			break;
		}
		requirements.add("name", CUSTOM);
	}

	@Override
	public Command clone() {
		final CreateCommand newCommand = new CreateCommand(context, componentType);
		newCommand.requirements = new Requirements<>(requirements);
		return newCommand;
	}

	@Override
	public void fillRequirements(Frame parent, Editor newContext) {
		context(newContext);

		// alter the `CUSTOM` type for this specific use
		CUSTOM.alter(constructRegex(), "Available, no spaces");

		// provide preset
		requirements.get("name").offer(context.getNextID(componentType));
		super.fillRequirements(parent, newContext);
	}

	 /**
	  * Creates a component
	  * 
	  * @throws CycleException if the creation of a branch leads to a cyclical (feedback) circuit
	  * @throws MissingComponentException if the ends of a branch aren't valid components
	  * @throws MalformedBranchException if a branch could not be properly created
	  */
	@Override
	public void execute() throws CycleException, MissingComponentException, MalformedBranchException {
		if (associatedComponent != null) {
			// when re-executed, simply restore the already-created Component
			context.addComponent(associatedComponent);
			ComponentFactory.restoreDeletedComponent(associatedComponent);
		} else {
			switch (componentType) {
			case INPUT_PIN:
				associatedComponent = ComponentFactory.createInputPin();
				break;
			case OUTPUT_PIN:
				associatedComponent = ComponentFactory.createOutputPin();
				break;
			case BRANCH:
				final Component in = context.getComponent_(requirements.getV("in id"));
				final Component out = context.getComponent_(requirements.getV("out id"));
				
				/*
				 * If connecting the Branch leads to a cycle being created, the connection is aborted.
				 * This has the (unintended) consequence of not checking whether or not the connection
				 * is valid in the first place (no MalformedBranchException is thrown).
				 * As a result, the user is, in some cases, warned that a cycle is going to be created
				 * but in reality such a connection is not valid, due to the nature of the Components
				 * being connected.
				 * This is a compromise we're willing to make since creating the Branch and then, if
				 * a cycle is created, deleting it would require additional code to restore everything,
				 * making this method needlessly complicated and hard to understand.
				 */
				if (!context.graph.componentCanBeConnected(in.getID(), out.getID()))
					throw new CycleException(in,out); 
				
				final int inIndex = Integer.parseInt(requirements.getV("in index"));
				final int outIndex = Integer.parseInt(requirements.getV("out index"));

				associatedComponent = ComponentFactory.connectComponents(in, inIndex, out, outIndex);
				break;
			case GATEAND:
			case GATEOR:
			case GATENOT:
			case GATEXOR:
				associatedComponent = ComponentFactory.createPrimitiveGate(componentType,
						Integer.parseInt(requirements.getV("in count")));
				break;
			case GATE:
				throw new RuntimeException(String.format(
						"Cannot directly create Components of type %s", componentType));
			default:
				break;
			}

			associatedComponent.setID(requirements.getV("name"));
			context.addComponent(associatedComponent);
			
			if(componentType != BRANCH) //notify graph if not notified already
				context.graph.componentAdded(associatedComponent.getID());
		}

		// delete the branch that may have been deleted when creating this branch
		// there can't be more than two branches deleted when creating a branch
		if (associatedComponent.type() == BRANCH) {
			final List<Component> ls = context.getDeletedComponents();

			if (ls.size() == 0)
				return;

			if (ls.size() > 1)
				throw new RuntimeException(
						"There can't be more than 1 deleted Branches after creating a Branch");

			final Command d = new DeleteCommand(context);
			deleteCommands.add(d);
			d.requirements.fulfil("id", String.valueOf(ls.get(0).getID()));

			try {
				// the Component with that id for sure exists; this statement can't throw
				d.execute();
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void unexecute() {
		// restore the previously deleted Branches
		if (componentType == BRANCH) {
			Utility.foreach(deleteCommands, Command::unexecute);
			deleteCommands.clear();
		}

		ComponentFactory.destroyComponent(associatedComponent);
		context.removeComponent(associatedComponent);
	}

	@Override
	public String toString() {
		return "Create " + componentType.description();
	}

	private String constructRegex() {
		// Construct the following regex: ^(?!foo$|bar$|)[^\s]*$
		// to match IDs that don't contain blanks and are not in use
		final StringBuilder regex = new StringBuilder("^(?!$");
		Utility.foreach(context.getComponents_(), c -> {
			regex.append("|");
			regex.append(c.getID());
			regex.append("$");
		});
		regex.append(")[^\\s]*$");
		return regex.toString();
	}
}
