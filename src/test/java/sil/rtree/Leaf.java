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
import java.util.Stack;

import sil.spatialindex.Region;
import sil.spatialindex.SpatialIndex;

public class Leaf extends Node
{
	public Leaf(RTree pTree, int id)
	{
		super(pTree, id, 0, pTree.m_leafCapacity);
	}

	protected Node chooseSubtree(Region mbr, int level, Stack pathBuffer)
	{
		return this;
	}

	protected Leaf findLeaf(Region mbr, int id, Stack pathBuffer)
	{
		for (int cChild = 0; cChild < m_children; cChild++)
		{
			if (m_pIdentifier[cChild] == id && mbr.equals(m_pMBR[cChild])) return this;
		}

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

		Node left = new Leaf(m_pTree, -1);
		Node right = new Leaf(m_pTree, -1);

		int cIndex;

		for (cIndex = 0; cIndex < g1.size(); cIndex++)
		{
			int i = ((Integer) g1.get(cIndex)).intValue();
			left.insertEntry(m_pData[i], m_pMBR[i], m_pIdentifier[i]);

			// we don't want to delete the data array from this node's destructor!
			m_pData[i] = null;
		}

		for (cIndex = 0; cIndex < g2.size(); cIndex++)
		{
			int i = ((Integer) g2.get(cIndex)).intValue();
			right.insertEntry(m_pData[i], m_pMBR[i], m_pIdentifier[i]);

			// we don't want to delete the data array from this node's destructor!
			m_pData[i] = null;
		}

		Node[] ret = new Node[2];
		ret[0] = left;
		ret[1] = right;
		return ret;
	}

	protected void deleteData(int id, Stack pathBuffer)
	{
		int child;
		for (child = 0; child < m_children; child++)
		{
			if (m_pIdentifier[child] == id) break;
		}

		deleteEntry(child);
		m_pTree.writeNode(this);

		Stack toReinsert = new Stack();
		condenseTree(toReinsert, pathBuffer);

		// re-insert eliminated nodes.
		while (! toReinsert.empty())
		{
			Node n = (Node) toReinsert.pop();
			m_pTree.deleteNode(n);

			for (int cChild = 0; cChild < n.m_children; cChild++)
			{
				// keep this in the for loop. The tree height might change after insertions.
				boolean[] overflowTable = new boolean[m_pTree.m_stats.m_treeHeight];
				for (int cLevel = 0; cLevel < m_pTree.m_stats.m_treeHeight; cLevel++) overflowTable[cLevel] = false;

				m_pTree.insertData_impl(n.m_pData[cChild],
																n.m_pMBR[cChild], n.m_pIdentifier[cChild],
																n.m_level, overflowTable);
				n.m_pData[cChild] = null;
			}
		}
	}
}
