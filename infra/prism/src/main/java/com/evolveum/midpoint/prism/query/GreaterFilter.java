package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class GreaterFilter extends ComparativeFilter{
	

	public GreaterFilter() {
	}
	
	GreaterFilter(ItemPath parentPath, ItemDefinition definition, PrismValue value, boolean equals) {
		super(parentPath, definition, value, equals);
	}
	
	public static GreaterFilter createGreaterFilter(ItemPath parentPath, ItemDefinition definition, PrismValue value, boolean equals){
		return new GreaterFilter(parentPath, definition, value, equals);
	}
	
	public static GreaterFilter createGreaterFilter(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, PrismValue value, boolean equals) throws SchemaException {
		return (GreaterFilter) createComparativeFilter(GreaterFilter.class, parentPath, containerDef, propertyName, value, equals);
	}

	public static GreaterFilter createGreaterFilter(ItemPath parentPath, ItemDefinition item, Object realValue, boolean equals) throws SchemaException{
		return (GreaterFilter) createComparativeFilter(GreaterFilter.class, parentPath, item, realValue, equals);
	}

	public static GreaterFilter createGreaterFilter(ItemPath parentPath, PrismContainerDefinition<? extends Containerable> containerDef,
			QName propertyName, Object realValue, boolean equals) throws SchemaException {
		return (GreaterFilter) createComparativeFilter(GreaterFilter.class, parentPath, containerDef, propertyName, realValue, equals);
	}

	public static GreaterFilter createGreaterFilter(Class<? extends Objectable> type, PrismContext prismContext, QName propertyName, Object realValue, boolean equals)
			throws SchemaException {
		return (GreaterFilter) createComparativeFilter(GreaterFilter.class, type, prismContext, propertyName, realValue, equals);
	}
	
	@Override
	public GreaterFilter clone() {
		return new GreaterFilter(getParentPath(), getDefinition(), getValues().get(0), isEquals());
	}

	@Override
	public String dump() {
		return debugDump(0);
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("GREATER: \n");
		
		if (getParentPath() != null){
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getParentPath().toString());
			sb.append("\n");
		} 
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DEF: ");
		if (getDefinition() != null) {
			sb.append(getDefinition().debugDump(indent));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE: ");
		if (getValues() != null) {
			indent += 1;
			for (PrismValue val : getValues()) {
				sb.append(val.debugDump(indent));
			}
		} else {
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null\n");
		}
		return sb.toString();
	}

}