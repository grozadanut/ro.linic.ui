package ro.linic.ui.legacy.tables.components;

public class Column
{
	private int index;
	private String property;
	private String name;
	private int size;
	
	public Column(final int index, final String property, final String name, final int size)
	{
		super();
		this.index = index;
		this.property = property;
		this.name = name;
		this.size = size;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getProperty()
	{
		return property;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public Column withSize(final int size)
	{
		return new Column(index, property, name, size);
	}

	public int getIndex()
	{
		return index;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Column other = (Column) obj;
		if (index != other.index)
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (property == null)
		{
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}
}
