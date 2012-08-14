package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;


@Embeddable
public class RSynchronizationSituationDescription implements Serializable{

	@QueryAttribute(enumerated = true)
	private RSynchronizationSituation situation;
	private XMLGregorianCalendar timestamp;
	private String chanel;
	
	
	@Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
	public RSynchronizationSituation getSituation() {
		return situation;
	}
	
	public void setSituation(RSynchronizationSituation situation) {
		this.situation = situation;
	}
	
	@Column(nullable = true)
	public XMLGregorianCalendar getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(XMLGregorianCalendar timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getChanel() {
		return chanel;
	}
	
	public void setChanel(String chanel) {
		this.chanel = chanel;
	}
	
	  @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;

	        RSynchronizationSituationDescription that = (RSynchronizationSituationDescription) o;

	        if (situation != null ? !situation.equals(that.situation) : that.situation != null) return false;
	        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
	        if (chanel != null ? !chanel.equals(that.chanel) : that.chanel != null) return false;

	        return true;
	    }

	    @Override
	    public int hashCode() {
	        int result = situation != null ? situation.hashCode() : 0;
	        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
	        result = 31 * result + (chanel != null ? chanel.hashCode() : 0);
	        return result;
	    }

	    @Override
	    public String toString() {
	        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	    }

	    public static RSynchronizationSituationDescription copyFromJAXB(SynchronizationSituationDescriptionType jaxb) {
	        if (jaxb == null) {
	            return null;
	        }

	        RSynchronizationSituationDescription repo = new RSynchronizationSituationDescription();
	        repo.setChanel(jaxb.getChannel());
	        repo.setTimestamp(jaxb.getTimestamp());
	        repo.setSituation(RSynchronizationSituation.toRepoType(jaxb.getSituation()));
	        
	        return repo;
	    }

	    public static SynchronizationSituationDescriptionType copyToJAXB(RSynchronizationSituationDescription repo) {
	        if (repo == null) {
	            return null;
	        }

	        SynchronizationSituationDescriptionType jaxb = new SynchronizationSituationDescriptionType();
	        jaxb.setChannel(repo.getChanel());
	        jaxb.setTimestamp(repo.getTimestamp());
	        jaxb.setSituation(repo.getSituation().getSyncType());
	       
	        return jaxb;
	    }
	
}