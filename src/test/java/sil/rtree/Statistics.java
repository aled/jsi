// Spatial Index Library
//
// Copyright (C) 2002  Navel Ltd.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Contact information:
//  Mailing address:
//    Marios Hadjieleftheriou
//    University of California, Riverside
//    Department of Computer Science
//    Surge Building, Room 310
//    Riverside, CA 92521
//
//  Email:
//    marioh@cs.ucr.edu

package sil.rtree;

import java.util.ArrayList;

import sil.spatialindex.IStatistics;

public class Statistics implements IStatistics, Cloneable
{
	protected long m_reads;
	protected long m_writes;
	protected long m_splits;
	protected long m_hits;
	protected long m_misses;
	protected long m_nodes;
	protected long m_adjustments;
	protected long m_queryResults;
	protected long m_data;
	protected int m_treeHeight;
	protected ArrayList m_nodesInLevel = new ArrayList();

	public Statistics()
	{
		reset();
	}

	public Statistics(Statistics s)
	{
		m_reads  = s.m_reads;
		m_writes = s.m_writes;
		m_splits = s.m_splits;
		m_hits   = s.m_hits;
		m_misses = s.m_misses;
		m_nodes  = s.m_nodes;
		m_adjustments = s.m_adjustments;
		m_queryResults = s.m_queryResults;
		m_data = s.m_data;
		m_treeHeight = s.m_treeHeight;
		m_nodesInLevel = (ArrayList) s.m_nodesInLevel.clone();
	}

	public long getReads()
	{
		return m_reads;
	}

	public long getWrites()
	{
		return m_writes;
	}

	public long getNumberOfNodes()
	{
		return m_nodes;
	}

	public long getNumberOfData()
	{
		return m_data;
	}

	public long getSplits()
	{
		return m_splits;
	}

	public long getHits()
	{
		return m_hits;
	}

	public long getMisses()
	{
		return m_misses;
	}

	public long getAdjustments()
	{
		return m_adjustments;
	}

	public long getQueryResults()
	{
		return m_queryResults;
	}

	public int getTreeHeight()
	{
		return m_treeHeight;
	}

	public int getNumberOfNodesInLevel(int l) throws IndexOutOfBoundsException
	{
		return ((Integer) m_nodesInLevel.get(l)).intValue();
	}

	public void reset()
	{
		m_reads  = 0;
		m_writes = 0;
		m_splits = 0;
		m_hits   = 0;
		m_misses = 0;
		m_nodes  = 0;
		m_adjustments = 0;
		m_queryResults = 0;
		m_data = 0;
		m_treeHeight = 0;
		m_nodesInLevel.clear();
	}

	public String toString()
	{
		String s = "Reads: " + m_reads + "\n" +
							 "Writes: " + m_writes + "\n" +
							 "Hits: " + m_hits + "\n" +
							 "Misses: " + m_misses + "\n" +
							 "Tree height: " + m_treeHeight + "\n" +
							 "Number of data: " + m_data + "\n" +
							 "Number of nodes: " + m_nodes + "\n";

		for (int cLevel = 0; cLevel < m_treeHeight; cLevel++)
		{
			s += "Level " + cLevel + " pages: " + ((Integer) m_nodesInLevel.get(cLevel)).intValue() + "\n";
		}

		s += "Splits: " + m_splits + "\n" +
				 "Adjustments: " + m_adjustments + "\n" +
				 "Query results: " + m_queryResults;

		return s;
	}

	public Object clone()
	{
		return new Statistics(this);
	}
}
