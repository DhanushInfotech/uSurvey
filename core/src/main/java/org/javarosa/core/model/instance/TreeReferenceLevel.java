/**
 * 
 */
package org.javarosa.core.model.instance;

import android.os.Parcel;
import android.os.Parcelable;

import org.javarosa.core.util.ArrayUtilities;
import org.javarosa.core.util.CacheTable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ctsims
 *
 */
public class TreeReferenceLevel implements Externalizable, Parcelable {
	public static final int MULT_UNINIT = -16;
	
	private String name;
	private int multiplicity = MULT_UNINIT;
	private List<XPathExpression> predicates;
	
	private static CacheTable<TreeReferenceLevel> refs;
	
	public static void attachCacheTable(CacheTable<TreeReferenceLevel> refs) {
		TreeReferenceLevel.refs = refs; 
	}

	public TreeReferenceLevel() {
	}
	
	
	public TreeReferenceLevel(String name, int multiplicity, List<XPathExpression> predicates) {
		this.name = name;
		this.multiplicity = multiplicity;
		this.predicates = predicates;
	}

	public TreeReferenceLevel(String name, int multiplicity) {
		this(name, multiplicity, null);
	}


	public int getMultiplicity() {
		return multiplicity;
	}

	public String getName() {
		return name;
	}

	public TreeReferenceLevel setMultiplicity(int mult) {
		return new TreeReferenceLevel(name, mult, predicates).intern();
	}

	public TreeReferenceLevel setPredicates(List<XPathExpression> xpe) {
		return new TreeReferenceLevel(name, multiplicity, xpe).intern();
	}

	public List<XPathExpression> getPredicates() {
		return this.predicates;
	}
	
	public TreeReferenceLevel shallowCopy() {
		return new TreeReferenceLevel(name, multiplicity, ArrayUtilities.listCopy(predicates)).intern();
	}


	public TreeReferenceLevel setName(String name) {
		return new TreeReferenceLevel(name, multiplicity, predicates).intern();
	}


	public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
		name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
		multiplicity = ExtUtil.readInt(in);
		predicates = (List<XPathExpression>) ExtUtil.nullIfEmpty((List<XPathExpression>)ExtUtil.read(in,new ExtWrapListPoly()));
	}


	public void writeExternal(DataOutputStream out) throws IOException {
		ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
		ExtUtil.writeNumeric(out, multiplicity);
		ExtUtil.write(out, new ExtWrapListPoly(ExtUtil.emptyIfNull(predicates)));
	}
	
	public int hashCode() {
		int predPart = 0;
		if(predicates != null) {
			for(XPathExpression xpe : predicates) {
				predPart ^= xpe.hashCode();
			}
		}
		
		return name.hashCode() ^ multiplicity ^ predPart; 
	}
	
	public boolean equals(Object o) {
		if(!(o instanceof TreeReferenceLevel)) {
			return false;
		}
		TreeReferenceLevel l = (TreeReferenceLevel)o;
		if(multiplicity != l.multiplicity) { return false;}
		if(name == null && l.name != null) { return false;}
		if(!name.equals(l.name)) { return false;}
		if(predicates == null && l.predicates == null) { return true; }
		
		if((predicates == null && l.predicates != null) || (l.predicates == null && predicates != null)) { return false; }
		if(predicates.size() != l.predicates.size()) { return false;}
		for(int i = 0 ; i < predicates.size() ; ++i) {
			if(!predicates.get(i).equals(l.predicates.get(i))) { return false; }
		}
		return true;
	}
	
	public static boolean treeRefLevelInterningEnabled = true;
	public TreeReferenceLevel intern() {
		if(!treeRefLevelInterningEnabled || refs == null) {
			return this;
		} else{
			return refs.intern(this);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.name);
		dest.writeInt(this.multiplicity);
		dest.writeList(this.predicates);
	}

	protected TreeReferenceLevel(Parcel in) {
		this.name = in.readString();
		this.multiplicity = in.readInt();
		this.predicates = new ArrayList<XPathExpression>();
		in.readList(this.predicates, List.class.getClassLoader());
	}

	public static final Parcelable.Creator<TreeReferenceLevel> CREATOR = new Parcelable.Creator<TreeReferenceLevel>() {
		public TreeReferenceLevel createFromParcel(Parcel source) {
			return new TreeReferenceLevel(source);
		}

		public TreeReferenceLevel[] newArray(int size) {
			return new TreeReferenceLevel[size];
		}
	};
}
