package ro.linic.ui.base.services.mapper;

import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.datachange.UpdateDataChange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ro.linic.ui.base.services.nattable.UpdateCommand;

@Mapper
public interface UpdateCommandMapper {
	UpdateCommandMapper INSTANCE = Mappers.getMapper(UpdateCommandMapper.class);
	
	@Mapping(target = "modelClass", source = "modelClass")
	@Mapping(target = "model", expression = "java( ((org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier) udc.getKey()).rowObject )")
	@Mapping(target = "updatedProperty", expression = 
	"java( cpa.getColumnProperty(((org.eclipse.nebula.widgets.nattable.datachange.IdIndexIdentifier) udc.getKey()).columnIndex) )")
	@Mapping(target = "newValue", source = "udc.value")
	UpdateCommand from(Class modelClass, IColumnPropertyAccessor cpa, UpdateDataChange udc);
}
