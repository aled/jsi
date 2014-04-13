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

package sil.spatialindex;

import java.io.FileNotFoundException;
import java.io.IOException;

import sil.storagemanager.DiskStorageManager;
import sil.storagemanager.IStorageManager;
import sil.storagemanager.MemoryStorageManager;
import sil.storagemanager.PropertySet;

public class SpatialIndex
{
	public static final String EMAIL = "marioh@cs.ucr.edu";
	public static final String VERSION = "0.44.2b";
	public static final String DATE = "27 July 2003";

	public static final double EPSILON = 1.192092896e-07;

	public static final int RtreeVariantQuadratic = 1;
	public static final int RtreeVariantLinear = 2;
	public static final int RtreeVariantRstar = 3;

	public static final int PersistentIndex = 1;
	public static final int PersistentLeaf = 2;

	public static final int ContainmentQuery = 1;
	public static final int IntersectionQuery = 2;

	public static ISpatialIndex createRTree(PropertySet ps, IStorageManager sm)
	{
		return null;
	}

	public static IStorageManager createMemoryStorageManager(PropertySet ps)
	{
		IStorageManager sm = (IStorageManager) new MemoryStorageManager();
		return sm;
	}

	public static IStorageManager createDiskStorageManager(PropertySet ps)
		throws SecurityException, NullPointerException, IOException, FileNotFoundException, IllegalArgumentException
	{
		IStorageManager sm = (IStorageManager) new DiskStorageManager(ps);
		return sm;
	}
} // SpatialIndex
