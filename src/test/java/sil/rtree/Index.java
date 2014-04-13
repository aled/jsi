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
// License aint with this library; if not, write to the Free Software
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

import sil.spatialindex.SpatialIndex;
import sil.spatialindex.Region;

public class Index extends Node
{
	public Index(RTree pTree, int id, int level)
	{
		super(pTree, id, level, pTree.m_indexCapacity);
	}

	protected Node chooseSubtree(Region mbr, int level, Stack pathBuffer)
	{
		if (m_level == level) return this;

		pathBuffer.push(new Integer(m_identifier));

		int child = 0;

		switch (m_pTree.m_treeVariant)
		{
			case SpatialIndex.RtreeVariantLinear:
			case SpatialIndex.RtreeVariantQuadratic:
				child = findLeastEnlargement(mbr);
				break;
			case SpatialIndex.RtreeVariantRstar:
				if (m_level == 1)
				{
					// if this node points to leaves...
					child = findLeastOverlap(mbr);
				}
				else
				{
					child = findLeastEnlargement(mbr);
				}
			break;
			default:
				throw new IllegalStateException("Unknown RTree variant.");
		}

		Node n = m_pTree.readNode(m_pIdentifier[child]);
		Node ret = n.chooseSubtree(mbr, level, pathBuffer);

		return ret;
	}

	protected Leaf findLeaf(Region mbr, int id, Stack pathBuffer)
	{
		pathBuffer.push(new Integer(m_identifier));

		for (int cChild = 0; cChild < m_children; cChild++)
		{
			if (m_pMBR[cChild].contains(mbr))
			{
				Node n = m_pTree.readNode(m_pIdentifier[cChild]);
				Leaf l = n.findLeaf(mbr, id, pathBuffer);
				if (l != null) return l;
			}
		}

		pathBuffer.pop();

		return null;
	}

	protected Node[] split(byte[] pData, Region mbr, int id)
	{
		m_pTree.m_stats.m_splits++;

		ArrayList g1 = new ArrayList(), g2 = new ArrayList();

		switch (m_pTree.m_treeVariant)
		{
			case SpatialIndex.RtreeVariantLinear:
			case SpatialIndex.RtreeVariantQuadratic:
				rtreeSplit(pData, mbr, id, g1, g2);
				break;
			case SpatialIndex.RtreeVariantRstar:
				rstarSplit(pData, mbr, id, g1, g2);
				break;
			default:
				throw new IllegalStateException("Unknown RTree variant.");
		}

		Node left = new Index(m_pTree, m_identifier, m_level);
		Node right = new Index(m_pTree, -1, m_level);

		int cIndex;

		for (cIndex = 0; cIndex < g1.size(); cIndex++)
		{
			int i = ((Integer) g1.get(cIndex)).intValue();
			left.insertEntry(null, m_pMBR[i], m_pIdentifier[i]);
		}

		for (cIndex = 0; cIndex < g2.size(); cIndex++)
		{
			int i = ((Integer) g2.get(cIndex)).intValue();
			right.insertEntry(null, m_pMBR[i], m_pIdentifier[i]);
		}

		Node[] ret = new Node[2];
		ret[0] = left;
		ret[1] = right;
		return ret;
	}

	protected int findLeastEnlargement(Region r)
	{
		double area = Double.POSITIVE_INFINITY;
		int best = -1;

		for (int cChild = 0; cChild < m_children; cChild++)
		{
			Region t = m_pMBR[cChild].combinedRegion(r);

			double a = m_pMBR[cChild].getArea();
			double enl = t.getArea() - a;

			if (enl < area)
			{
				area = enl;
				best = cChild;
			}
			else if (enl == area)
			{
				if (a < m_pMBR[best].getArea()) best = cChild;
			}
		}

		return best;
	}

	protected int findLeastOverlap(Region r)
	{
		OverlapEntry[] entries = new OverlapEntry[m_children];

		double leastOverlap = Double.POSITIVE_INFINITY;
		double me = Double.POSITIVE_INFINITY;
		int best = -1;

		// find combined region and enlargement of every entry and store it.
		for (int cChild = 0; cChild < m_children; cChild++)
		{
			OverlapEntry e = new OverlapEntry();

			e.m_id = cChild;
			e.m_original = m_pMBR[cChild];
			e.m_combined = m_pMBR[cChild].combinedRegion(r);
			e.m_oa = e.m_original.getArea();
			e.m_ca = e.m_combined.getArea();
			e.m_enlargement = e.m_ca - e.m_oa;
			entries[cChild] = e;

			if (e.m_enlargement < me)
			{
				me = e.m_enlargement;
				best = cChild;
			}
			else if (e.m_enlargement == me && e.m_oa < entries[best].m_oa)
			{
				best = cChild;
			}
		}

		if (me < SpatialIndex.EPSILON || me > SpatialIndex.EPSILON)
		{
			int cIterations;

			if (m_children > m_pTree.m_nearMinimumOverlapFactor)
			{
				// sort entries in increasing order of enlargement.
				Arrays.sort(entries, new OverlapEntryComparator());
				cIterations = m_pTree.m_nearMinimumOverlapFactor;
			}
			else
			{
				cIterations = m_children;
			}

			// calculate overlap of most important original entries (near minimum overlap cost).
			for (int cIndex = 0; cIndex < cIterations; cIndex++)
			{
				double dif = 0.0;
				OverlapEntry e = entries[cIndex];

				for (int cChild = 0; cChild < m_children; cChild++)
				{
					if (e.m_id != cChild)
					{
						double f = e.m_combined.getIntersectingArea(m_pMBR[cChild]);
						if (f != 0.0) dif +=  f - e.m_original.getIntersectingArea(m_pMBR[cChild]);
					}
				} // for (cChild)

				if (dif < leastOverlap)
				{
					leastOverlap = dif;
					best = cIndex;
				}
				else if (dif == leastOverlap)
				{
					if (e.m_enlargement == entries[best].m_enlargement)
					{
						// keep the one with least area.
						if (e.m_original.getArea() < entries[best].m_original.getArea()) best = cIndex;
					}
					else
					{
						// keep the one with least enlargement.
						if (e.m_enlargement < entries[best].m_enlargement) best = cIndex;
					}
				}
			} // for (cIndex)
		}

		return entries[best].m_id;
	}

	protected void adjustTree(Node n, Stack pathBuffer)
	{
		m_pTree.m_stats.m_adjustments++;

		// find entry pointing to old node;
		int child;
		for (child = 0; child < m_children; child++)
		{
			if (m_pIdentifier[child] == n.m_identifier) break;
		}

		// MBR needs recalculation if either:
		//   1. the NEW child MBR is not contained.
		//   2. the OLD child MBR is touching.
		boolean b = m_nodeMBR.contains(n.m_nodeMBR);
		boolean recalc = (! b) ? true : m_nodeMBR.touches(m_pMBR[child]);

		m_pMBR[child] = (Region) n.m_nodeMBR.clone();

		if (recalc)
		{
			for (int cDim = 0; cDim < m_pTree.m_dimension; cDim++)
			{
				m_nodeMBR.m_pLow[cDim] = Double.POSITIVE_INFINITY;
				m_nodeMBR.m_pHigh[cDim] = Double.NEGATIVE_INFINITY;

				for (int cChild = 0; cChild < m_children; cChild++)
				{
					m_nodeMBR.m_pLow[cDim] = Math.min(m_nodeMBR.m_pLow[cDim], m_pMBR[cChild].m_pLow[cDim]);
					m_nodeMBR.m_pHigh[cDim] = Math.max(m_nodeMBR.m_pHigh[cDim], m_pMBR[cChild].m_pHigh[cDim]);
				}
			}
		}

		m_pTree.writeNode(this);

		if (recalc && ! pathBuffer.empty())
		{
			int cParent = ((Integer) pathBuffer.pop()).intValue();
			Index p = (Index) m_pTree.readNode(cParent);
			p.adjustTree(this, pathBuffer);
		}
	}

	protected void adjustTree(Node n1, Node n2, Stack pathBuffer, boolean[] overflowTable)
	{
		m_pTree.m_stats.m_adjustments++;

		// find entry pointing to old node;
		int child;
		for (child = 0; child < m_children; child++)
		{
			if (m_pIdentifier[child] == n1.m_identifier) break;
		}

		// MBR needs recalculation if either:
		//   1. the NEW child MBR is not contained.
		//   2. the OLD child MBR is touching.
		boolean b = m_nodeMBR.contains(n1.m_nodeMBR);
		boolean recalc = (! b) ? true : m_nodeMBR.touches(m_pMBR[child]);

		m_pMBR[child] = (Region) n1.m_nodeMBR.clone();

		if (recalc)
		{
			for (int cDim = 0; cDim < m_pTree.m_dimension; cDim++)
			{
				m_nodeMBR.m_pLow[cDim] = Double.POSITIVE_INFINITY;
				m_nodeMBR.m_pHigh[cDim] = Double.NEGATIVE_INFINITY;

				for (int cChild = 0; cChild < m_children; cChild++)
				{
					m_nodeMBR.m_pLow[cDim] = Math.min(m_nodeMBR.m_pLow[cDim], m_pMBR[cChild].m_pLow[cDim]);
					m_nodeMBR.m_pHigh[cDim] = Math.max(m_nodeMBR.m_pHigh[cDim], m_pMBR[cChild].m_pHigh[cDim]);
				}
			}
		}

		// No write necessary here. insertData will write the node if needed.
		//m_pTree.writeNode(this);

		boolean adjusted = insertData(null, (Region) n2.m_nodeMBR.clone(), n2.m_identifier, pathBuffer, overflowTable);

		// if n2 is contained in the node and there was no split or reinsert,
		// we need to adjust only if recalculation took place.
		// In all other cases insertData above took care of adjustment.
		if (! adjusted && recalc && ! pathBuffer.empty())
		{
			int cParent = ((Integer) pathBuffer.pop()).intValue();
			Index p = (Index) m_pTree.readNode(cParent);
			p.adjustTree(this, pathBuffer);
		}
	}

	class OverlapEntry
	{
		int m_id;
		double m_enlargement;
		Region m_original;
		Region m_combined;
		double m_oa;
		double m_ca;
	}

	class OverlapEntryComparator implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			OverlapEntry e1 = (OverlapEntry) o1;
			OverlapEntry e2 = (OverlapEntry) o2;

			if (e1.m_enlargement < e2.m_enlargement) return -1;
			if (e1.m_enlargement > e2.m_enlargement) return 1;
			return 0;
		}
	}
}
