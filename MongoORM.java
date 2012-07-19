
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoORM 
{
	protected Map<Object, ObjectId> pickled = new HashMap<Object, ObjectId>();
	protected Map<ObjectId, Object> depickled = new HashMap<ObjectId, Object>();
	protected DB db;
	
	public MongoORM(DB db)
	{
		this.db = db;
	}
	//----- save -----
	public void save(Object o) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException 
	{
		Class<?> clazz = o.getClass();
		DBCollection collection = getCollectionName(clazz);
		if(collection == null)
		{
			return;
		}
		DBObject dbobject = new BasicDBObject();
		
		// field name & value
		List<Field> fields = getAllFields(clazz);
		for(Field f: fields)
		{
			Annotation a = f.getAnnotation(MongoField.class);
			Object fieldobj = f.get(o);
			// check for field annotation
			if(a == null || fieldobj == null)
			{
				continue;
			}
			// convert value for DB
			fieldobj = valueSerial(fieldobj);
			if(fieldobj == null)
			{
				continue;
			}
			String name = getAnnotationFieldName(f);
			dbobject.put(name, fieldobj);
		}
		
		// put into DB
		ObjectId oid = getObjectId(o);
		DBObject tmp = new BasicDBObject();
		tmp.put("_id", oid);
		collection.update(tmp, dbobject);
	}
	
	private DBCollection getCollectionName(Class<?> clazz) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Annotation collectionAnno = clazz.getAnnotation(MongoCollection.class);
		if(collectionAnno == null)
		{
			return null;
		}
		String collectionName = (String) MongoCollection.class.getMethod("value").invoke(collectionAnno);		
		DBCollection collection = db.getCollection(getAnnoName(clazz.getName(), collectionName));
		
		return collection;
	}
	
	private String getAnnotationFieldName(Field f) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		Annotation fieldAnno = f.getAnnotation(MongoField.class);
		String fieldName = (String) MongoField.class.getMethod("value").invoke(fieldAnno);		
		fieldName = getAnnoName(f.getName(), fieldName);
		
		return fieldName;
	}
	
	private String getAnnoName(String memberName, String annoName)
	{
		if(annoName != null && !annoName.isEmpty())
		{
			return annoName;
		}
		return memberName;
	}
	
	private List<Field> getAllFields(Class<?> clazz) 
	{
		List<Field> list = new ArrayList<Field>();
		
		while(clazz != null)
		{
			if(clazz.getAnnotation(MongoCollection.class) != null)
			{
				list.addAll(Arrays.asList(clazz.getDeclaredFields()));
			}
			clazz = clazz.getSuperclass();
		}
		
		return list;
	}
	
	private ObjectId getObjectId(Object o) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		ObjectId id = null;
		if(pickled.containsKey(o))
		{
			id = pickled.get(o);
		}
		else
		{
			DBCollection collection = getCollectionName(o.getClass());
			DBObject tmp = new BasicDBObject();
			collection.save(tmp);
			id = getDBId(collection, tmp);
			if(id != null)
			{
				pickled.put(o, id);
			}
		}
		return id;
	}
	
	private ObjectId getDBId(DBCollection collection, DBObject dbo)
	{
		DBCursor curr = collection.find(dbo);
		if(curr.count() == 0)
		{
			System.err.println("cannot find this object");
			return null; 
		}
		ObjectId id = (ObjectId) curr.next().get("_id");
		return id;
	}
	
	private Object valueSerial(Object o) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		if(o instanceof List || o instanceof Set || o instanceof Queue)
		{
			o = addObjType(convertListObj(((Collection<?>) o).toArray()), o.getClass().getSimpleName());
			return o;
		}
		if(o instanceof Map)
		{
			List<DBObject> maplist = new ArrayList<DBObject>();
			for(Object i: ((Map<?, ?>) o).keySet())
			{	
				DBObject dbmap = new BasicDBObject();
				Object inkey = valueSerial(i);
				Object invalue = valueSerial(((Map<?, ?>) o).get(i));
				dbmap.put("key", inkey);
				dbmap.put("value", invalue);
				maplist.add(dbmap);
			}
			if(!maplist.isEmpty())
			{
				o = addObjType(maplist, o.getClass().getSimpleName());
				return o;
			}
			return null;
		}
		if(o.getClass().getAnnotation(MongoCollection.class) != null)
		{
			o = addObjType(getObjectId(o), o.getClass().getName());
			return o;
		}
		if(checkValidType(o.getClass()))
		{
			return o;
		}
		return null;
	}
	
	private Object convertListObj(Object[] oArray) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		for(int i = 0; i < oArray.length; i++)
		{
			Object tmp = valueSerial(oArray[i]);
			if(tmp != null)
			{
				oArray[i] = tmp;
			}
		}
		return Arrays.asList(oArray);
	}
	
	private Object addObjType(Object o, String type)
	{
		DBObject dbo = new BasicDBObject();
		dbo.put("type", type);
		dbo.put("obj", o);
		return dbo;
	}
	
	private boolean checkValidType(Class<?> clazz)
	{
		if(clazz.equals(Integer.class) || 
			clazz.equals(Double.class) ||
			clazz.equals(Float.class) ||
			clazz.equals(Boolean.class) ||
			clazz.equals(String.class) || 
			clazz.equals(Date.class))
		{
			return true;
		}		
		return false;
	}

	//----- load -----
	@SuppressWarnings("unchecked")
	public <T> List<T> loadAll(Class<T> clazz) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException
	{
		DBCollection collection = getCollectionName(clazz);
		List<T> objList = new ArrayList<T>();
		DBCursor curr = collection.find();
		while(curr.hasNext())
		{
			// new object of the clazz
			DBObject dbo = curr.next();			
			ObjectId id = (ObjectId) dbo.get("_id");
			T o = null;
			
			if(depickled.containsKey(id))
			{
				o = (T) depickled.get(id);	
			}
			else
			{
				o = (T) loadOne(clazz, dbo, id);
			}
			objList.add(o);
		}
		return objList;
	}
	
	private <T> Object loadOne(Class<T> clazz, DBObject dbo, ObjectId id) throws InstantiationException, IllegalAccessException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException
	{
		T o = clazz.newInstance();
		depickled.put(id, o);
		List<Field> fields = getAllFields(o.getClass());
		for(Field f: fields)
		{
			Annotation a = f.getAnnotation(MongoField.class);
			if(a == null)
			{
				continue;
			}
			String name = getAnnotationFieldName(f);
			Object value = dbo.get(name);
			if(value != null)
			{
				value = valueDeserial(dbo, name);
			}
			f.set(o, value);
		}
		return o;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object valueDeserial(DBObject dbo, String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException
	{
		Object o = dbo.get(name);
		Class<?> clazz = o.getClass();
		if(!clazz.equals(BasicDBObject.class))
		{
			return o;
		}
		
		DBObject objvalue = (DBObject) dbo.get(name);
		String type = (String) objvalue.get("type");
		Object obj = objvalue.get("obj");
		if(obj == null)
		{
			return o;
		}
		if(obj.getClass().equals(BasicDBList.class))
		{
			BasicDBList objObj = (BasicDBList) objvalue.get("obj");
			Class<?> c = Class.forName("java.util."+type);
			Object newObj = c.newInstance();
			if(newObj instanceof List || newObj instanceof Set || newObj instanceof Queue)
			{
				Collection tmp = (Collection<?>) newObj;
				for(Object i: objObj)
				{
					if(checkValidType(i.getClass()))
					{
						tmp.add(i);
					}
					else
					{
						tmp.add(getDBObject((DBObject) i));
					}
				}				
				newObj = tmp;
			}
			else if(newObj instanceof Map)
			{
				Map tmp = (Map<?, ?>) newObj;
				for(Object i: objObj)
				{
					DBObject j = (DBObject) i;
					Object inkey = valueDeserial(j, "key");
					Object invalue = valueDeserial(j, "value");
					tmp.put(inkey, invalue);
				}
				newObj = tmp;
			}
			return newObj;
		}
		if(Class.forName(type).getAnnotation(MongoCollection.class) != null)
		{
			o = getDBObject(objvalue);
		}
		return o;
	}
	
	private Object getDBObject(DBObject objvalue) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, IllegalArgumentException, NoSuchMethodException, InvocationTargetException
	{
		Object value = null;
		ObjectId objid = (ObjectId) objvalue.get("obj");
		if(depickled.containsKey(objid))
		{
			value = depickled.get(objid);
		}
		else
		{
			Class<?> c = Class.forName((String) objvalue.get("type"));
			DBObject dbid = new BasicDBObject();
			dbid.put("_id", objid);
			DBCursor curr = getCollectionName(c).find(dbid);
			if(curr.count() == 0)
			{
				System.err.println("cannot find this object");
				return null; 
			}
			value = loadOne(c, curr.next(), objid);
		}
		return value;
	}
}
