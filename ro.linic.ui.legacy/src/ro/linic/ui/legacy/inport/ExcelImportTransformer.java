package ro.linic.ui.legacy.inport;

import static ro.colibri.util.NumberUtils.parse;
import static ro.colibri.util.PresentationUtils.EMPTY_STRING;
import static ro.colibri.util.PresentationUtils.NBSP_S;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import ro.colibri.embeddable.Address;
import ro.colibri.embeddable.FidelityCard;
import ro.colibri.embeddable.ProductGestiuneMappingId;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.mappings.ProductGestiuneMapping;
import ro.colibri.entities.user.User;
import ro.colibri.util.LocalDateUtils;
import ro.colibri.util.ServerConstants;
import ro.colibri.wrappers.RulajPartener;

public class ExcelImportTransformer {
	
	public static ImmutableSet<Product> toProducts(final InputStream inputStream, final ImmutableList<Gestiune> gestiuni) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		final Gestiune l1 = gestiuni.stream()
				.filter(gestiune -> gestiune.isMatch(ServerConstants.L1_NAME))
				.findFirst().get();
		final Gestiune l2 = gestiuni.stream()
				.filter(gestiune -> gestiune.isMatch(ServerConstants.L2_NAME))
				.findFirst().get();
		
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<Product> products = ImmutableSet.<Product>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final ProductGestiuneMapping l1Mapping = new ProductGestiuneMapping();
        		l1Mapping.setGestiune(l1);
        		l1Mapping.setId(new ProductGestiuneMappingId(null, l1.getId()));
        		final ProductGestiuneMapping l2Mapping = new ProductGestiuneMapping();
        		l2Mapping.setGestiune(l2);
        		l2Mapping.setId(new ProductGestiuneMappingId(null, l2.getId()));
                final Product product = new Product();
                l1Mapping.setProduct(product);
                l2Mapping.setProduct(product);
                product.getStocuri().add(l1Mapping);
                product.getStocuri().add(l2Mapping);

                for (int i = 0; i <= 16; i++)
                	putCellValueInProduct(i, product, l1Mapping, l2Mapping, currentRow);
                
                products.add(product);
            }
            
            return products.build();
        }
	}
	
	private static void putCellValueInProduct(final int i, final Product product, final ProductGestiuneMapping l1Mapping, final ProductGestiuneMapping l2Mapping, final Row row) throws ImportParseException
    {
		final String textCellValue;
		double numericCellValue;
		final Cell cell = row.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);
    	switch (i)
    	{
	    	case 0: // activ
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.BOOLEAN) && !cell.getCellType().equals(CellType.FORMULA))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie BOOLEAN!");
	    		
	    		product.setActiv(cell.getBooleanCellValue());
	    		break;
	    		
	    	case 2: // category
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setCategorie(textCellValue);
        		break;
    		
        	case 5: // BARCODE
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setBarcode(textCellValue);
        		break;
        		
        	case 6: // NAME
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setName(textCellValue);
        		break;
        		
        	case 8: // UOM
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setUom(textCellValue);
        		break;
        		
        	case 9: // ulpFTva
        		if (cell == null)
        			return;
        		
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		product.setLastBuyingPriceNoTva(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 10: // Price/UOM
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 6 cifre inainte de virgula "+numericCellValue);
        		
        		product.setPricePerUom(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 11: // STOC L1
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		l1Mapping.setStoc(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 12: // STOC L2
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		l2Mapping.setStoc(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 13: // Min stoc
        		if (cell == null)
        			return;
        		
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		product.setMinStoc(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 1: // gestiune(MARGHITA)
        	case 3: // TVA%
        	case 4: // GRP
        	case 7: // luni
        	case 14: // MC/BUC
        	case 15: // PretMed/ValMij
        	case 16: // idx
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<Operatiune> toOperations(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (final Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<Operatiune> operations = ImmutableSet.<Operatiune>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;

                final Operatiune operation = new Operatiune();
				operation.setAccDoc(new AccountingDocument());
                
                for (int i = 0; i <= 23; i++)
                	putCellValueInOperation(i, operation, currentRow);
                
                operations.add(operation);
            }
            
            return operations.build();
        }
	}
	
	private static void putCellValueInOperation(final int i, final Operatiune operation, final Row row) throws ImportParseException
    {
		final String textCellValue;
		double numericCellValue;
		Date dateCellValue;
		final Cell cell = row.getCell(i);
    	switch (i)
    	{
	    	case 0: // rpz
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.BOOLEAN) && !cell.getCellType().equals(CellType.FORMULA))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie BOOLEAN!");
	    		
	    		operation.setRpz(cell.getBooleanCellValue());
    		break;
    		
        	case 1: // Tip Operatiune(intrare/iesire)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setTipOp(TipOp.parse(textCellValue));
        		break;
        		
        	case 2: // category(MARFA)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setCategorie(textCellValue);
        		break;
        		
        	case 5: // Cod de bare
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setBarcode(textCellValue);
        		break;
        		
        	case 6: // Denumire
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setName(textCellValue);
        		break;
        		
        	case 7: // UM
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setUom(textCellValue);
        		break;
        		
        	case 8: // Gestiune op (L1, L2)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setGestiune(new Gestiune().setImportName(textCellValue));
        		break;
        		
        	case 9: // Cantitate
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setCantitate(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 10: // PUA fTVA
        		if (cell == null)
        			return;
//        			throw new ImportParseException("Randul "+(row.getRowNum()+1)+" nu are PUA fTVA");
        		
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setPretUnitarAchizitieFaraTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 11: // VA fTVA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 9999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 10 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setValoareAchizitieFaraTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 12: // VA TVA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setValoareAchizitieTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 13: // PV+TVA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setPretVanzareUnitarCuTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 14: // VV fTVA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 9999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 10 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setValoareVanzareFaraTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 15: // VV TVA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 99999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 8 cifre inainte de virgula "+numericCellValue);
        		
        		operation.setValoareVanzareTVA(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 16: // Gestiune doc (L1, L2)
        		if (cell == null)
        		{
        			if (!operation.getName().equalsIgnoreCase("DISCOUNT OFERIT"))
        				throw new ImportParseException("Randul "+(row.getRowNum()+1)+" nu are gestiune doc");
        			operation.getAccDoc().setGestiune(operation.getGestiune());
        			break;
        		}
        		
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.getAccDoc().setGestiune(new Gestiune().setImportName(textCellValue));
        		break;
        		
        	case 17: // DOC(Factura, Bon de casa...)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.getAccDoc().setDoc(textCellValue);
        		break;
        		
        	case 18: // Nr doc
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.getAccDoc().setNrDoc(textCellValue);
        		break;
        		
        	case 19: // Data doc
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie DATE!");
        		
        		dateCellValue = cell.getDateCellValue();
        		operation.getAccDoc().setDataDoc(LocalDateUtils.toLocalDateTime(dateCellValue));
        		break;
        		
        	case 20: // Partener
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.getAccDoc().setPartner(new Partner().setName(textCellValue));
        		break;
        		
        	case 21: // Operator
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setOperator(new User().setName(textCellValue));
        		break;
        		
        	case 22: // DateTime Operatiunii
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie DATETIME!");
        		
        		dateCellValue = cell.getDateCellValue();
        		operation.setDataOp(LocalDateUtils.toLocalDateTime(dateCellValue));
        		break;
        		
        	case 23: // IDX
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		operation.setIdx(textCellValue);
        		break;
        		
        	// Columns we don't give a shit about
        	case 3: // %
        	case 4: // GRP
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<Partner> toPartners(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<Partner> partners = ImmutableSet.<Partner>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final Partner partner = new Partner();
                for (int i = 0; i <= 8; i++)
                	putCellValueInPartner(i, partner, currentRow);
                
                partners.add(partner);
            }
            
            return partners.build();
        }
	}
	
	private static void putCellValueInPartner(final int i, final Partner partner, final Row currentRow) throws ImportParseException
    {
		final String textCellValue;
    	final Cell currentCell = currentRow.getCell(i);
		switch (i)
    	{
        	case 0: // nr crt
        		break;
        		
        	case 1: // nume
        		checkNull(currentCell, currentRow, i);
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setName(textCellValue);
        		break;
        		
        	case 2: // cui
        		checkNull(currentCell, currentRow, i);
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setCodFiscal(textCellValue);
        		break;
        		
        	case 3: // reg com
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setRegCom(textCellValue);
        		break;
        		
        	case 4: // sediu
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		final Address address = new Address();
        		address.setOras(textCellValue);
        		partner.setAddress(address);
        		break;
        		
        	case 5: // judet
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		if (partner.getAddress() == null)
        			partner.setAddress(new Address());
        			
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.getAddress().setJudet(textCellValue);
        		break;
        		
        	case 6: // phone
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setPhone(textCellValue);
        		break;
        		
        	case 7: // banca
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setBanca(textCellValue);
        		break;
        		
        	case 8: // iban
        		if (currentCell == null)
        			return;
        		
        		if (!currentCell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(currentCell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+currentCell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = currentCell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setIban(textCellValue);
        		break;
        		
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<Partner> toMesteri(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<Partner> partners = ImmutableSet.<Partner>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final Partner partner = new Partner();
                for (int i = 0; i <= 4; i++)
                	putCellValueInMester(i, partner, currentRow);
                
                partners.add(partner);
            }
            
            return partners.build();
        }
	}
	
	private static void putCellValueInMester(final int i, final Partner partner, final Row row) throws ImportParseException
    {
		final String textCellValue;
		final Cell cell = row.getCell(i);
    	switch (i)
    	{
	    	case 0: // nume
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		partner.setName(textCellValue);
	    		break;
    		
        	case 1: // nr card
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		final FidelityCard fidelityCard = new FidelityCard();
        		fidelityCard.setNumber(textCellValue);
        		partner.setFidelityCard(fidelityCard);
        		break;
        		
        	case 2: // disc percentage
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.getFidelityCard().setDiscountPercentage(new BigDecimal(textCellValue));
        		break;
        		
        	case 3: // phone
        		if (cell == null)
        			return;
        		
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		partner.setPhone(textCellValue);
        		break;
        		
        	case 4: // adresa
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		final Address address = new Address();
        		address.setOras(textCellValue);
        		partner.setAddress(address);
        		break;
        		
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<DocumentWithDiscount> toClientDocs(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<DocumentWithDiscount> discountDocs = ImmutableSet.<DocumentWithDiscount>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final DocumentWithDiscount doc = new DocumentWithDiscount();
                
                for (int i = 0; i <= 6; i++)
                	putCellValueInDoc(i, doc, currentRow);

                discountDocs.add(doc);
            }
            
            return discountDocs.build();
        }
	}
	
	private static void putCellValueInDoc(final int i, final DocumentWithDiscount doc, final Row row) throws ImportParseException
    {
		String textCellValue;
		final Cell cell = row.getCell(i);
    	switch (i)
    	{
	    	case 0: // partner
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		doc.setPartner(new Partner().setName(textCellValue));
	    		break;
	    		
	    	case 1: // tipDoc
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		doc.setTipDoc(TipDoc.parse(textCellValue));
	    		break;
	    		
	    	case 2: // name
	    		if (cell == null)
        			return;
	    		
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		doc.setName(textCellValue);
	    		break;
	    		
	    	case 3: // Data Doc
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		doc.setDataDoc(LocalDateTime.parse(textCellValue));
        		break;
        		
	    	case 4: // operator
	    		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		doc.setOperator(new User().setName(textCellValue));
        		break;
        		
	    	case 5: // total
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		doc.setTotal(parse(textCellValue));
        		doc.setTotalTva(BigDecimal.ZERO);
        		break;
        		
        	case 6: // disc percentage
        		if (cell == null)
        			return;
        		
        		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		doc.setDiscountPercentage(parse(textCellValue));
        		break;
        		
        	default:
        		break;
    	}
    }
	
	
	public static ImmutableList<AccountingDocument> toAccDocs(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final com.google.common.collect.ImmutableList.Builder<AccountingDocument> docs = ImmutableList.<AccountingDocument>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final AccountingDocument accDoc = new AccountingDocument();
                
                for (int i = 0; i <= 17; i++)
                	putCellValueInAccDoc(i, accDoc, currentRow);
                
                docs.add(accDoc);
            }
            
            return docs.build();
        }
	}
	
	private static void putCellValueInAccDoc(final int i, final AccountingDocument accDoc, final Row row) throws ImportParseException
    {
		final String textCellValue;
		final double numericCellValue;
		final Date dateCellValue;
		final Cell cell = row.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);
    	switch (i)
    	{
	    	case 1: // L12
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		accDoc.setGestiune(new Gestiune().setImportName(textCellValue));
    		break;
    		
	    	case 2: // partener
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		accDoc.setPartner(new Partner().setName(textCellValue));
    		break;
    		
        	case 3: // TipDoc(V, C, P, I)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setTipDoc(TipDoc.parse(textCellValue));
        		break;
        		
        	case 4: // Doc(Factura, Chitanta, Bon Consum...)
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setDoc(textCellValue);
        		break;
        		
        	case 5: // Nr Doc
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setNrDoc(textCellValue);
        		break;
        		
        	case 6: // Data Doc
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie DATE!");
        		
        		dateCellValue = cell.getDateCellValue();
        		accDoc.setDataDoc(LocalDateUtils.toLocalDateTime(dateCellValue));
        		break;
        		
        	case 7: // Descriere
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setName(textCellValue);
        		break;
        		
        	case 9: // total
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		accDoc.setTotal(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 10: // total tva
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 12 cifre inainte de virgula "+numericCellValue);
        		
        		accDoc.setTotalTva(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	case 13: // rpz
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.BOOLEAN) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie BOOLEAN!");
        		
        		accDoc.setRpz(cell.getBooleanCellValue());
        		break;
        		
        	case 14: // casa
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.BOOLEAN) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie BOOLEAN!");
        		
        		accDoc.setRegCasa(cell.getBooleanCellValue());
        		break;
        		
        	case 15: // banca
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.BOOLEAN) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie BOOLEAN!");
        		
//        		accDoc.setRegBanca(cell.getBooleanCellValue());
        		break;
        		
        	case 16: // operator
        		if (cell == null)
        			return;
        		
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setOperator(new User().setName(textCellValue));
        		break;
        		
        	case 17: // idx
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		accDoc.setIdx(textCellValue);
        		break;
        		
        	// Columns we don't give a shit about
        	case 0: // gestiunea MARGHITA
        	case 8: // TVA%
        	case 11: // val RPZ
        	case 12: // val RPZ tva
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<RulajPartener> toRulaje(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<RulajPartener> discountDocs = ImmutableSet.<RulajPartener>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final RulajPartener rulaj = new RulajPartener();
                
                for (int i = 0; i <= 9; i++)
                	putCellValueInRulaj(i, rulaj, currentRow);

                discountDocs.add(rulaj);
            }
            
            return discountDocs.build();
        }
	}
	
	private static void putCellValueInRulaj(final int i, final RulajPartener rulaj, final Row row) throws ImportParseException
    {
		String textCellValue;
		double numericCellValue;
		final Cell cell = row.getCell(i);
    	switch (i)
    	{
	    	case 0: // partner
	    		checkNull(cell, row, i);
	    		if (!cell.getCellType().equals(CellType.STRING))
	    			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(cell.getColumnIndex()+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
	    		
	    		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
	    		rulaj.setName(textCellValue);
	    		break;
	    		
	    	case 2: // si de plata
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setSi_dePlata(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 3: // RULAJ  ACH
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setRulajAch(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 4: // RULAJ PLATI
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setRulajPlati(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 5: // DE PLATA
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setDePlata(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 6: // SI-DE INCAS
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setSi_deIncas(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 7: // RULA VANZ
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setRulajVanz(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
	    		
	    	case 8: // RULAJ INCAS
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setRulajIncas(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
	    	case 9: // DE INCASAT
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		if (numericCellValue > 99999999999999D)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 14 cifre inainte de virgula "+numericCellValue);
        		
        		rulaj.setDeIncasat(BigDecimal.valueOf(numericCellValue).setScale(2, RoundingMode.HALF_EVEN));
        		break;
        		
        	default:
        		break;
    	}
    }
	
	public static ImmutableSet<Product> toShortProducts(final InputStream inputStream) throws ImportParseException, EncryptedDocumentException, InvalidFormatException, IOException
	{
		try (Workbook workbook = WorkbookFactory.create(inputStream))
		{
            final Sheet datatypeSheet = workbook.getSheetAt(0);
            final Iterator<Row> rowIterator = datatypeSheet.iterator();
            final Builder<Product> products = ImmutableSet.<Product>builder();

            while (rowIterator.hasNext())
            {
                final Row currentRow = rowIterator.next();
                if (!currentRow.cellIterator().hasNext())
                	continue;
                
                final Product product = new Product();

                for (int i = 0; i <= 16; i++)
                	putCellValueInShortProduct(i, product, currentRow);
                
                products.add(product);
            }
            
            return products.build();
        }
	}
	
	private static void putCellValueInShortProduct(final int i, final Product product, final Row row) throws ImportParseException
    {
		final String textCellValue;
		double numericCellValue;
		final Cell cell = row.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);
    	switch (i)
    	{
        	case 0: // BARCODE
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setBarcode(textCellValue);
        		break;
        		
        	case 1: // NAME
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setName(textCellValue);
        		break;
        		
        	case 2: // UOM
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.STRING))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie TEXT!");
        		
        		textCellValue = cell.getStringCellValue().replaceAll(NBSP_S, EMPTY_STRING).trim();
        		product.setUom(textCellValue);
        		break;
        		
        	case 3: // Price/UOM
        		checkNull(cell, row, i);
        		if (!cell.getCellType().equals(CellType.NUMERIC) && !cell.getCellType().equals(CellType.FORMULA))
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" este de tip "+cell.getCellType()+", desi ar trebui sa fie NUMERIC!");
        		
        		numericCellValue = cell.getNumericCellValue();
        		
        		if (numericCellValue > 999999)
        			throw new ImportParseException("Randul "+(cell.getRowIndex()+1)+" coloana "+(i+1)+" trebuie sa aiba maxim 6 cifre inainte de virgula "+numericCellValue);
        		
        		product.setPricePerUom(BigDecimal.valueOf(numericCellValue));
        		break;
        		
        	default:
        		break;
    	}
    }

	private static void checkNull(final Cell cell, final Row row, final int i) throws ImportParseException
	{
		if (cell == null)
			throw new ImportParseException("Randul "+(row.getRowNum()+1)+" coloana "+(i+1)+" este de GOALA!");
	}
}
