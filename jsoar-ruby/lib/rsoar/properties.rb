
class Java::OrgJsoarUtilProperties::PropertyManager
  
  def [] (key)
    get key
  end
  
  def []= (key, value)
    set key, value
  end
end