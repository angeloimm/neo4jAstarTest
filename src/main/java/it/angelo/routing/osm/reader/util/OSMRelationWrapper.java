package it.angelo.routing.osm.reader.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class OSMRelationWrapper extends OSMElement
{

	private Relation osmRelation;
	private List<RelationMember> members;
	private Long osmRelationId;

	public OSMRelationWrapper(Relation osmRelation)
	{

		this.osmRelation = osmRelation;
		this.osmRelationId = osmRelation.getId();
		members = this.osmRelation.getMembers();
		Collection<Tag> relationTags = osmRelation.getTags();
		tags = new HashMap<String, String>(relationTags.size());
		for (Tag tag : relationTags)
		{

			tags.put(tag.getKey(), tag.getValue());
		}
	}

	public boolean isMetaRelation()
	{
		for (RelationMember member : members)
		{

			if (member.getMemberType() == EntityType.Relation)
			{
				return true;
			}
		}
		return false;
	}

	public Relation getOsmRelation()
	{

		return osmRelation;
	}

	public Long getOsmRelationId()
	{
		return osmRelationId;
	}
}
