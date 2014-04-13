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

package sil.storagemanager;

import java.util.*;

public abstract class Buffer implements IBuffer
{
	int m_capacity = 10;
	boolean m_bWriteThrough = false;
	IStorageManager m_storageManager = null;
	HashMap m_buffer = new HashMap();
	long m_hits = 0;

	abstract void addEntry(int id, Entry entry);
	abstract void removeEntry();

	public Buffer(IStorageManager sm, int capacity, boolean bWriteThrough)
	{
		m_storageManager = sm;
		m_capacity = capacity;
		m_bWriteThrough = bWriteThrough;
	}

	public byte[] loadByteArray(final int id)
	{
		byte[] ret = null;
		Entry e = (Entry) m_buffer.get(new Integer(id));

		if (e != null)
		{
			m_hits++;

			ret = new byte[e.m_data.length];
			System.arraycopy(e.m_data, 0, ret, 0, e.m_data.length);
  	}
		else
		{
			ret = m_storageManager.loadByteArray(id);
			e = new Entry(ret);
			addEntry(id, e);
		}

		return ret;
	}
	public int storeByteArray(final int id, final byte[] data)
	{
		int ret = id;

		if (id == NewPage)
  	{
 			ret = m_storageManager.storeByteArray(id, data);
 			Entry e = new Entry(data);
			addEntry(ret, e);
  	}
  	else
  	{
  		if (m_bWriteThrough)
			{
				m_storageManager.storeByteArray(id, data);
			}

			Entry e = (Entry) m_buffer.get(new Integer(id));
			if (e != null)
			{
				e.m_data = new byte[data.length];
				System.arraycopy(data, 0, e.m_data, 0, data.length);

				if (m_bWriteThrough == false)
				{
					e.m_bDirty = true;
					m_hits++;
				}
				else
				{
					e.m_bDirty = false;
				}
			}
			else
			{
				e = new Entry(data);
				if (m_bWriteThrough == false) e.m_bDirty = true;
  			addEntry(id, e);
			}
		}

		return ret;
	}
	public void deleteByteArray(final int id)
	{
		Integer ID = new Integer(id);
		Entry e = (Entry) m_buffer.get(ID);
		if (e != null)
		{
			m_buffer.remove(ID);
		}

		m_storageManager.deleteByteArray(id);
	}

	public void flush()
	{
		Iterator it = m_buffer.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry me = (Map.Entry) it.next();
			Entry e = (Entry) me.getValue();
			int id = ((Integer) me.getKey()).intValue();
			if (e.m_bDirty) m_storageManager.storeByteArray(id, e.m_data);
		}

		m_storageManager.flush();
	}
	public void clear()
	{
		Iterator it = m_buffer.entrySet().iterator();

		while (it.hasNext())
		{
			Map.Entry me = (Map.Entry) it.next();
			Entry e = (Entry) me.getValue();

			if (e.m_bDirty)
			{
				int id = ((Integer) me.getKey()).intValue();
				m_storageManager.storeByteArray(id, e.m_data);
			}
		}

		m_buffer.clear();
		m_hits = 0;
	}

	public long getHits()
	{
		return m_hits;
	}

	class Entry
	{
		byte[] m_data = null;
		boolean m_bDirty = false;

		Entry(final byte[] d)
		{
			m_data = new byte[d.length];
			System.arraycopy(d, 0, m_data, 0, d.length);
		}
	}; // Entry

} // Buffer
