package ro.linic.ui.legacy.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ro.colibri.entities.comercial.Product;

@Mapper
public interface ProductMapper {
	ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);
	
	@Mapping(target = "barcodes", ignore = true)
	@Mapping(target = "id", source = "p.id")
	@Mapping(target = "type", source = "p.categorie")
	@Mapping(target = "taxCode", source = "p.department.cotaTva")
	@Mapping(target = "departmentCode", source = "p.department.name")
	@Mapping(target = "sku", source = "p.barcode")
	@Mapping(target = "name", source = "p.name")
	@Mapping(target = "uom", source = "p.uom")
	@Mapping(target = "stockable", expression = "java( ro.colibri.entities.comercial.Product.shouldModifyStoc(p) )")
	@Mapping(target = "price", source = "p.pricePerUom")
	@Mapping(target = "stock", expression = "java( "
			+ "p.stoc(ro.linic.ui.legacy.session.ClientSession.instance().getLoggedUser().getSelectedGestiune()) )")
	@Mapping(target = "imageId", source = "p.imageUUID")
	@Mapping(target = "taxPercentage", source = "p.department.tvaPercentage")
	ro.linic.ui.pos.base.model.Product from(Product p);
}
