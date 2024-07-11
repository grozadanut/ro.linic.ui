package ro.linic.ui.legacy.session;

import static ro.colibri.util.ListUtils.toImmutableList;
import static ro.colibri.util.ListUtils.toImmutableSet;
import static ro.colibri.util.StringUtils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import jakarta.ejb.EJBException;
import ro.colibri.beans.ImportBean;
import ro.colibri.beans.ImportBeanRemote;
import ro.colibri.beans.LoginBean;
import ro.colibri.beans.LoginBeanRemote;
import ro.colibri.beans.MailSender;
import ro.colibri.beans.MailSenderRemote;
import ro.colibri.beans.ManagerBean;
import ro.colibri.beans.ManagerBeanRemote;
import ro.colibri.beans.StatisticsBean;
import ro.colibri.beans.StatisticsBeanRemote;
import ro.colibri.beans.UsersBean;
import ro.colibri.beans.UsersBeanRemote;
import ro.colibri.beans.VanzariBean;
import ro.colibri.beans.VanzariBeanRemote;
import ro.colibri.embeddable.Delegat;
import ro.colibri.entities.comercial.AccountingDocument;
import ro.colibri.entities.comercial.AccountingDocument.BancaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CasaLoad;
import ro.colibri.entities.comercial.AccountingDocument.ContaLoad;
import ro.colibri.entities.comercial.AccountingDocument.CoveredDocsLoad;
import ro.colibri.entities.comercial.AccountingDocument.DocumentTypesLoad;
import ro.colibri.entities.comercial.AccountingDocument.LoadBonuri;
import ro.colibri.entities.comercial.AccountingDocument.RPZLoad;
import ro.colibri.entities.comercial.AccountingDocument.TransportType;
import ro.colibri.entities.comercial.CIM;
import ro.colibri.entities.comercial.CasaDepartment;
import ro.colibri.entities.comercial.ContBancar;
import ro.colibri.entities.comercial.Document;
import ro.colibri.entities.comercial.Document.TipDoc;
import ro.colibri.entities.comercial.DocumentWithDiscount;
import ro.colibri.entities.comercial.Gestiune;
import ro.colibri.entities.comercial.GrupaInteres;
import ro.colibri.entities.comercial.LobImage;
import ro.colibri.entities.comercial.Masina;
import ro.colibri.entities.comercial.Operatiune;
import ro.colibri.entities.comercial.Operatiune.Recenzie;
import ro.colibri.entities.comercial.Operatiune.TipOp;
import ro.colibri.entities.comercial.Operatiune.TransferType;
import ro.colibri.entities.comercial.Partner;
import ro.colibri.entities.comercial.PersistedProp;
import ro.colibri.entities.comercial.PontajZilnic;
import ro.colibri.entities.comercial.Product;
import ro.colibri.entities.comercial.ProductUiCategory;
import ro.colibri.entities.comercial.Raion;
import ro.colibri.entities.comercial.Reducere;
import ro.colibri.entities.comercial.TempDocument;
import ro.colibri.entities.comercial.mappings.ProductRecipeMapping;
import ro.colibri.entities.user.AuditEvent;
import ro.colibri.entities.user.AuditEvent.AuditEventType;
import ro.colibri.entities.user.Company;
import ro.colibri.entities.user.Permission;
import ro.colibri.entities.user.Role;
import ro.colibri.entities.user.User;
import ro.colibri.security.SecurityUtils;
import ro.colibri.util.InvocationResult;
import ro.colibri.wrappers.ClasamentEntry;
import ro.colibri.wrappers.LastYearStats;
import ro.colibri.wrappers.PontajLine;
import ro.colibri.wrappers.ProductProfitability;
import ro.colibri.wrappers.ProductTurnover;
import ro.colibri.wrappers.RaionProfitability;
import ro.colibri.wrappers.RulajPartener;
import ro.colibri.wrappers.SalesPerHours;
import ro.colibri.wrappers.SalesPerOperators;
import ro.colibri.wrappers.ThreeEntityWrapper;
import ro.linic.ui.legacy.components.AsyncLoadData;
import ro.linic.ui.legacy.components.AsyncLoadResult;
import ro.linic.ui.legacy.preferences.PreferenceKey;

public class BusinessDelegate
{
	private static ILog log = ILog.of(BusinessDelegate.class);
	
	private static void fillImageCache(final Bundle bundle, final Logger log, final ImmutableSet<String> uuids)
	{
		final ImmutableSet<String> unfoundUUIDs = uuids.stream()
				.filter(uuid -> !isEmpty(uuid))
				.filter(uuid -> !Icons.imgRelativePathExists(bundle, log, productImageFilename(uuid)))
				.collect(toImmutableSet());
		
		if (!unfoundUUIDs.isEmpty())
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			bean.getImageLobs(unfoundUUIDs.toArray(new String[] {})).stream()
			.forEach(lob -> {
				try
				{
					Icons.saveImageBytesToRelativeFile(bundle, productImageFilename(lob.getImageUUID()), lob.getImage());
				}
				catch (final Exception e)
				{
					log.error(e);
				}
			});
		}
	}
	
	public static String productImageFilename(final String uuid)
	{
		return "produse/"+uuid+".jpeg";
	}
	
	public static ImmutableList<User> dbUsers()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.allUsers();
	}
	
	public static ImmutableMap<User, List<Company>> usersWithCompanyRoles()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.usersWithCompanyRoles();
	}
	
	public static ImmutableMap<Company, List<Gestiune>> companiesWithGestiuni()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.companiesWithGestiuni();
	}
	
	public static ImmutableList<Role> dbRoles()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.roleChoices();
	}
	
	public static ImmutableList<Masina> dbMasini()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.allMasini();
	}
	
	public static InvocationResult createUser_ByAdmin(final User userToAdd, final long selectedRoleId, final int selectedGestiuneId,
			final ImmutableSet<Long> masiniIds, final boolean faraLogin)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.createUser_ByAdmin(userToAdd, selectedRoleId, selectedGestiuneId, masiniIds, faraLogin);
	}
	
	public static User userById(final int userId)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.userById(userId);
	}
	
	public static void updateUser(final User user, final boolean faraLogin)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		usersBean.updateUser(user, faraLogin);
	}
	
	public static User login()
	{
		try {
			ServiceLocator.clearCache();
			final LoginBeanRemote loginBean = ServiceLocator.getBusinessService(LoginBean.class, LoginBeanRemote.class);
			return loginBean.loggedUser();
		} catch (final Exception e) {
			log.error(e.getMessage(), e);
			// try offline login
			return loginUserOffline();
		}
	}
	
	private static User loginUserOffline() {
		final Bundle bundle = FrameworkUtil.getBundle(PreferenceKey.class);
		final ISecurePreferences root = SecurePreferencesFactory.getDefault();
 		final ISecurePreferences secureNode = root.node(bundle.getSymbolicName());
 		
 		try {
 			final ObjectMapper objectMapper = new ObjectMapper();
			final String userHash = SecurityUtils.hashSha512(ClientSession.instance().getUsername() +
					ClientSession.instance().getPassword());
			final String foundUser = secureNode.get(userHash, null);
			return isEmpty(foundUser) ? null : objectMapper.readValue(foundUser, new TypeReference<User>(){});
		} catch (final StorageException | JsonProcessingException e) {
			log.error("Error getting secure preferences", e);
		}
		
		return null;
	}

	public static InvocationResult changeGestiune(final int userId, final int gestiuneId)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.changeGestiune(userId, gestiuneId);
	}
	
	public static InvocationResult removeUser(final int userId)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.removeUser(userId);
	}
	
	public static ImmutableList<Permission> allUserVisPermissions()
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.allUserVisPermissions();
	}
	
	public static InvocationResult deleteRole(final long roleId)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.deleteRole(roleId);
	}
	
	public static InvocationResult persistNewRole(final Role role, final ImmutableSet<String> permissions)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.persistNewRole(role, permissions);
	}
	
	public static InvocationResult updateRole(final long roleId, final ImmutableSet<String> permissions, final String name)
	{
		final UsersBeanRemote usersBean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return usersBean.updateRole(roleId, permissions, name);
	}
	
	public static InvocationResult persistGestiune(final String name, final String importName)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistGestiune(name, importName);
	}
	
	public static InvocationResult updateGestiune(final int gestId, final String name, final String importName)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateGestiune(gestId, name, importName);
	}
	
	public static InvocationResult persistMasina(final String nr, final String marca, final String culoare, final int gestiuneId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistMasina(nr, marca, culoare, gestiuneId);
	}
	
	public static InvocationResult updateMasina(final long masinaId, final String nr, final String marca, final String culoare, final int gestiuneId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateMasina(masinaId, nr, marca, culoare, gestiuneId);
	}
	
	public static InvocationResult deleteMasina(final long masinaId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.deleteMasina(masinaId);
	}
	
	public static long autoNumber(final TipDoc tipDoc, final String doc, final Integer gestiuneId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.autoNumber(tipDoc, doc, gestiuneId);
	}
	
	public static void allProducts(final AsyncLoadData<Product> provider, final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		final Job job = Job.create("Loading All Products", (ICoreRunnable) monitor ->
		{
			try
			{
				final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
				final ImmutableList<Product> allProducts = bean.allProducts();
				if (!monitor.isCanceled())
				{
					sync.asyncExec(() -> provider.success(allProducts));
					fillImageCache(bundle, log, allProducts.stream()
							.map(Product::getImageUUID)
							.filter(Objects::nonNull)
							.collect(toImmutableSet()));
				}
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server: "+e.getMessage()));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
	}
	
	public static void allProductsForOrdering(final AsyncLoadData<Product> provider, final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		final Job job = Job.create("Loading All Products for ordering", (ICoreRunnable) monitor ->
		{
			try
			{
				final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
				final ImmutableList<Product> allProducts = bean.allProductsForOrdering();
				if (!monitor.isCanceled())
				{
					sync.asyncExec(() -> provider.success(allProducts));
					fillImageCache(bundle, log, allProducts.stream()
							.map(Product::getImageUUID)
							.filter(Objects::nonNull)
							.collect(toImmutableSet()));
				}
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server: "+e.getMessage()));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
	}
	
	public static void allProducts_InclInactive(final AsyncLoadData<Product> provider, final UISynchronize sync, final Bundle bundle, final Logger log)
	{
		final Job job = Job.create("Loading All Products(Manager)", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<Product> allProducts = bean.allProducts_InclInactive();
				if (!monitor.isCanceled())
				{
					sync.asyncExec(() -> provider.success(allProducts));
					fillImageCache(bundle, log, allProducts.stream()
							.map(Product::getImageUUID)
							.filter(Objects::nonNull)
							.collect(toImmutableSet()));
				}
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server: "+e.getMessage()));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
	}
	
	public static ImmutableList<Product> convertToProducts(final ImmutableList<Operatiune> ops, final Bundle bundle, final Logger log)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.convertToProducts(ops);
	}
	
	public static void bonuriZiuaCurenta(final AsyncLoadData<AccountingDocument> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading Bonuri Ziua Curenta", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote commercialBean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final ImmutableList<AccountingDocument> bonuri = commercialBean.bonuriZiuaCurenta();
			sync.asyncExec(() -> provider.success(bonuri));
		});

		job.schedule();
	}
	
	public static Job filteredDocs(final AsyncLoadData<AccountingDocument> provider, final UISynchronize sync, 
			final TipDoc tipDoc, final LocalDate from, final LocalDate to, final LoadBonuri loadType, final Logger log)
	{
		final Job job = Job.create("Loading Filtered Docs", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<AccountingDocument> docs = bean.filteredOperationDocs(tipDoc, from, to, loadType);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(docs));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job filteredOperations(final AsyncLoadData<Operatiune> provider, final UISynchronize sync, final TipOp tipOp,
			final String category, final LocalDate from, final LocalDate to, final ImmutableSet<String> docs, final String nrDoc,
			final Partner partner, final String barcode, final String name, final Gestiune gestiuneOp, final Gestiune gestiuneDoc,
			final User user, final int maxRows, final LocalDate fromRec, final LocalDate toRec, final Logger log)
	{
		final Job job = Job.create("Loading Filtered Operations", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<Operatiune> ops = bean.filteredOperations(tipOp, category, from, to, docs, nrDoc, partner,
						barcode, name, gestiuneOp, gestiuneDoc, user, maxRows, fromRec, toRec);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(ops));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	/**
	 * @param gestiuneId @nullable
	 * @param partnerId @nullable
	 * @param tipDoc @nullable
	 * @param from @notnull
	 * @param to @notnull
	 * @param rpzLoad @notnull
	 * @param casaLoad @notnull
	 * @param documentTypes @notnull
	 * @return
	 */
	public static Job filteredDocuments(final AsyncLoadData<Document> provider, final UISynchronize sync, final Integer gestiuneId,
			final Long partnerId, final TipDoc tipDoc, final LocalDate from, final LocalDate to, final RPZLoad rpzLoad, final CasaLoad casaLoad,
			final BancaLoad bancaLoad, final Integer contBancarId, final DocumentTypesLoad documentTypes, final CoveredDocsLoad coveredLoad,
			final Boolean shouldTransport, final Integer userId, final ContaLoad contaLoad, final LocalDate transportFrom,
			final LocalDate transportTo, final Logger log)
	{
		final Job job = Job.create("Loading Filtered Documents", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<Document> docs = bean.filteredDocuments(gestiuneId, partnerId, tipDoc, from, to, rpzLoad,
						casaLoad, bancaLoad, contBancarId, documentTypes, coveredLoad, shouldTransport, userId, contaLoad,
						transportFrom, transportTo);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(docs));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Product productById(final Integer id, final Bundle bundle, final Logger log)
	{
		final VanzariBeanRemote commercialBean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		final Product product = commercialBean.productById(id);
		if (product != null && !isEmpty(product.getImageUUID()))
			fillImageCache(bundle, log, ImmutableSet.of(product.getImageUUID()));
		return product;
	}
	
	public static InvocationResult addToBonCasa(final String productBarcode, final BigDecimal cantitate, final BigDecimal overridePret,
			final Long accDocId, final boolean negativeAllowed, final TransferType transferType, final String overrideName,
			final Bundle bundle, final Logger log)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		final InvocationResult result = bean.addToBonCasa(productBarcode, cantitate, overridePret, accDocId, negativeAllowed, transferType, overrideName);
		return result;
	}
	
	public static ImmutableList<AccountingDocument> unfinishedBonuriCasa()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.unfinishedBonuriCasa();
	}
	
	public static InvocationResult deleteOperations(final ImmutableSet<Long> operationIds)
	{
		if (operationIds.isEmpty())
			return InvocationResult.ok();

		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.deleteOperations(operationIds);
	}
	
	public static InvocationResult transferOperations(final ImmutableSet<Long> operationIds, final int otherGestId)
	{
		if (operationIds.isEmpty())
			return InvocationResult.ok();
	
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.transferOperations(operationIds, otherGestId);
	}
	
	public static ImmutableList<Operatiune> mergeOperations(final ImmutableSet<Operatiune> operations)
	{
		if (operations.isEmpty())
			return ImmutableList.of();

		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.mergeOperations(operations).extra(InvocationResult.OPERATIONS_KEY);
	}
	
	public static InvocationResult modifyOperationAntet(final TipDoc tipDoc, final String doc, final String nrDoc, final LocalDate dataDoc, final String nrRec,
			final LocalDate dataRec, final Long partnerId, final boolean rpz, final ImmutableSet<Long> opIds)
	{
		if (opIds.isEmpty())
			return null;

		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.modifyOperationAntet(tipDoc, doc, nrDoc, dataDoc, nrRec, dataRec, partnerId, rpz, opIds);
	}
	
	public static InvocationResult addOperationToUnpersistedDoc(final TipDoc tipDoc, final String doc, final String nrDoc, final LocalDate dataDoc, final String nrRec,
			final LocalDate dataRec, final Long partnerId, final boolean rpz, final Operatiune newOp, final Integer otherTransferGestId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.addOperationToUnpersistedDoc(tipDoc, doc, nrDoc, dataDoc, nrRec, dataRec, partnerId, rpz, newOp, otherTransferGestId);
	}
	
	public static InvocationResult addOperationToDoc(final long docId, final Operatiune op, final Integer otherTransferGestId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.addOperationToDoc(docId, op, otherTransferGestId);
	}
	
	/**
	 * @param doc @nullable
	 * @return can return null if the parameter is null or if the acc doc was deleted from the database
	 */
	public static AccountingDocument reloadDoc(final AccountingDocument doc)
	{
		if (doc == null)
			return null;

		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.accountingDocumentById(doc.getId(), false);
	}
	
	public static AccountingDocument reloadDoc(final Long docId)
	{
		if (docId == null)
			return null;

		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.accountingDocumentById(docId, false);
	}
	
	/**
	 * @param doc @nullable
	 * @return can return null if the parameter is null or if the acc doc was deleted from the database
	 */
	public static AccountingDocument reloadDoc(final AccountingDocument doc, final boolean loadConnectionOps)
	{
		if (doc == null)
			return null;

		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.accountingDocumentById(doc.getId(), loadConnectionOps);
	}
	
	public static InvocationResult saveLocalOps(final ImmutableList<Operatiune> localOps)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.saveLocalOps(localOps);
	}
	
	public static InvocationResult closeBonCasa_Failed(final ImmutableSet<Long> bonIds)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.closeBonCasa_Failed(bonIds);
	}
	
	public static InvocationResult closeBonCasa_RetrySuccess(final ImmutableSet<Long> bonIds)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.closeBonCasa_RetrySuccess(bonIds);
	}
	
	public static InvocationResult closeBonCasa(final long bonId, final boolean casaActive)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.closeBonCasa(bonId, casaActive);
	}
	
	public static InvocationResult closeFacturaBCAviz(final long bonId, final String docType, final BigDecimal achitat,
			final ContBancar contBancar, final boolean casaActiva, final String paidDocNr, final boolean addFidelityPoints)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.closeFacturaBCAviz(bonId, docType, achitat, contBancar, casaActiva, paidDocNr, addFidelityPoints);
	}
	
	public static ImmutableList<Partner> allPartners()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.allPartners();
	}
	
	public static void allPartners_InclInactive(final AsyncLoadData<Partner> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading All Partners(incl inactive)", (ICoreRunnable) monitor ->
		{
			final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
			final ImmutableList<Partner> allPartners = bean.allPartners_InclInactive();
			sync.asyncExec(() -> provider.success(allPartners));
		});

		job.schedule();
	}
	
	public static InvocationResult updateDelegat(final long partnerId, final Delegat delegat)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.updateDelegat(partnerId, delegat);
	}
	
	public static InvocationResult changeDocPartner(final long bonId, final Optional<Long> partnerId, final boolean loadAccDoc)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.changeDocPartner(bonId, partnerId.orElse(null), loadAccDoc);
	}
	
	public static InvocationResult updateDeliveryAddress(final Optional<Long> partnerId, final String phone, final String indicatii)
	{
		if (partnerId.isPresent())
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			return bean.updateDeliveryAddress(partnerId.get(), phone, indicatii);
		}
		return InvocationResult.ok();
	}
	
	public static InvocationResult verifyPartnerAtAnaf(final long partnerId)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.verifyPartner(partnerId);
	}
	
	public static InvocationResult verifyPartnerAtAnaf(final Partner partner)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.verifyPartner(partner);
	}
	
	public static InvocationResult setPartnerActive(final long partnerId, final boolean active)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.setPartnerActive(partnerId, active);
	}
	
	public static InvocationResult mergePartner(final Partner partner)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.mergePartner(partner);
	}
	
	public static InvocationResult deletePartner(final long partnerId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.deletePartner(partnerId);
	}
	
	public static InvocationResult inregistreazaZ(final String bfz, final LocalDate date, final BigDecimal discount,
			final BigDecimal numerar, final ImmutableMap<Integer, BigDecimal> contBancarIdToAmount)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.inregistreazaZ(bfz, date, discount, numerar, contBancarIdToAmount);
	}
	
	public static InvocationResult inregistreazaZ(final String bfz, final LocalDate date, final BigDecimal discount,
			final BigDecimal prodFinit, final BigDecimal marfa, final BigDecimal servicii, final BigDecimal bacsis, final BigDecimal totalTva,
			final ImmutableMap<Integer, BigDecimal> contBancarIdToAmount, final int nrCafele)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.inregistreazaZ(bfz, date, discount, prodFinit, marfa, servicii, bacsis, totalTva, contBancarIdToAmount, nrCafele);
	}
	
	public static void customerDebtDocs(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading Customer Debts", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final InvocationResult result = bean.customerDebtDocs();
			sync.asyncExec(() -> provider.success(result));
		});

		job.schedule();
	}
	
	public static void supplierDebtDocs(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading Supplier Debts", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final InvocationResult result = bean.supplierDebtDocs();
			sync.asyncExec(() -> provider.success(result));
		});

		job.schedule();
	}
	
	public static void puncteFidelitate(final AsyncLoadData<RulajPartener> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading Puncte Fidelitate", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final ImmutableList<RulajPartener> partners = bean.puncteFidelitate();
			if (!monitor.isCanceled())
				sync.asyncExec(() -> provider.success(partners));
		});

		job.schedule();
	}
	
	public static ImmutableList<RulajPartener> puncteFidelitate_Sync()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.puncteFidelitate();
	}
	
	public static InvocationResult incaseaza(final ImmutableSet<Long> vanzareDocIds, final Long partnerId, final BigDecimal achitat,
			final ContBancar contBancar, final boolean casaActiva, final boolean persist, final String paidDocNr,
			final boolean transformaInFactura, final boolean addDiscountDoc, final BigDecimal discFolosit, final LocalDate dataDoc,
			final ImmutableSet<Long> tempDocIds)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.incaseazaDocsOrPartner(vanzareDocIds, partnerId, achitat, contBancar, casaActiva, persist, paidDocNr,
				transformaInFactura, addDiscountDoc, discFolosit, dataDoc, tempDocIds);
	}
	
	public static InvocationResult platesteDoc(final Long accDocId, final BigDecimal achitat, final boolean regCasa, final ContBancar contBancar,
			final String paidDocDoc, final String paidDocNr, final LocalDateTime paidDocData, final boolean persist)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.platesteDoc(accDocId, achitat, regCasa, contBancar, paidDocDoc, paidDocNr, paidDocData, persist);
	}
	
	public static InvocationResult platestePartner(final Long partnerId, final BigDecimal achitat, final boolean regCasa, final ContBancar contBancar,
			final String paidDocDoc, final String paidDocNr, final LocalDateTime paidDocData, final boolean persist)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.platestePartner(partnerId, achitat, regCasa, contBancar, paidDocDoc, paidDocNr, paidDocData, persist);
	}
	
	public static InvocationResult platesteDocs(final ImmutableSet<Long> accDocIds, final Long partnerId, final BigDecimal achitat, final boolean regCasa,
			final ContBancar contBancar, final String paidDocDoc, final String paidDocNr, final LocalDateTime paidDocData, final boolean persist)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.platesteDocs(accDocIds, partnerId, achitat, regCasa, contBancar, paidDocDoc, paidDocNr, paidDocData, persist);
	}
	
	public static ImmutableList<Gestiune> allGestiuni()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.allGestiuni().stream()
				.sorted(Comparator.comparing(Gestiune::getImportName))
				.collect(toImmutableList());
	}
	
	public static ImmutableList<Gestiune> allGestiuni(final Integer companyId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.allGestiuni(companyId).stream()
				.sorted(Comparator.comparing(Gestiune::getImportName))
				.collect(toImmutableList());
	}
	
	public static InvocationResult persistProduct(final Product product)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistProduct(product);
	}
	
	public static InvocationResult mergeProduct(final Product product)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.mergeProduct(product);
	}
	
	public static InvocationResult deleteProduct(final int productId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.deleteProduct(productId);
	}
	
	public static InvocationResult eliminateProductFromOrdering(final int productId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.eliminateProductFromOrdering(productId);
	}
	
	public static InvocationResult persistAccDoc(final AccountingDocument accDoc, final long partnerId, final int gestiuneId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistAccDoc(accDoc, partnerId, gestiuneId);
	}
	
	public static InvocationResult persistDiscountDoc(final DocumentWithDiscount discountDoc, final long partnerId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistDiscountDoc(discountDoc, partnerId);
	}
	
	public static InvocationResult mergeAccDoc(final AccountingDocument accDoc)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.mergeAccDoc(accDoc);
	}
	
	public static InvocationResult deleteDiscountDoc(final long discountDocId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.deleteDiscountDoc(discountDocId);
	}
	
	public static InvocationResult deleteAccDoc(final long accDocId)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.deleteAccDoc(accDocId);
	}
	
	public static InvocationResult updateAccDocConnections(final long ownerAccDocId, final ImmutableMap<Long, BigDecimal> paysAmounts,
			final ImmutableMap<Long, BigDecimal> paidAmounts)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateAccDocConnections(ownerAccDocId, paysAmounts, paidAmounts);
	}
	
	public static InvocationResult updateReteta(final int productFinitId, final Collection<ProductRecipeMapping> ingredients)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateReteta(productFinitId, ingredients);
	}
	
	public static Job rulajeParteneri(final AsyncLoadData<RulajPartener> provider, final UISynchronize sync, final Integer gestiuneId,
			final Long partnerId, final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Rulaje Parteneri", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<RulajPartener> rulaje = bean.rulajeParteneri(gestiuneId, partnerId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(rulaje));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job regIncasariPlati(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync, final Integer gestiuneId,
			final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Reg Incasari Plati", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final InvocationResult result = bean.regIncasariPlati(gestiuneId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job regCasa(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync, final Integer gestiuneId,
			final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Reg Casa", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final InvocationResult result = bean.regCasa(gestiuneId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job regRPZ(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync, final Integer gestiuneId,
			final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Reg RPZ", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final InvocationResult result = bean.regRPZ(gestiuneId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job regBanca(final AsyncLoadResult<InvocationResult> provider, final UISynchronize sync,
			final Integer gestiuneId, final Integer contBancarId, final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Reg Banca", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final InvocationResult result = bean.regBanca(gestiuneId, contBancarId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static void updateFirmaDetails(final InvocationResult firmaDetails)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		bean.updateFirmaDetails(firmaDetails);
	}
	
	public static InvocationResult firmaDetails()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.firmaDetails();
	}
	
	public static PersistedProp persistedProp(final String key)
	{
		return ClientSession.instance().allPersistedProps().stream()
				.filter(prop -> prop.getKey().equalsIgnoreCase(key))
				.findFirst()
				.orElseGet(() ->
				{
					final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
					return bean.getPersistedProp(key);
				});
	}
	
	public static ImmutableList<PersistedProp> allPersistedProps()
	{
		return ClientSession.instance().allPersistedProps();
	}
	
	static ImmutableList<PersistedProp> allPersistedProps_NO_CACHE()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.allPersistedProps();
	}
	
	public static ImmutableList<PersistedProp> mergePersistedProps(final ImmutableSet<PersistedProp> props)
	{
		if (props.isEmpty())
			return ImmutableList.of();

		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		final ImmutableList<PersistedProp> res = bean.mergePersistedProps(props).extra(InvocationResult.PERSISTED_PROP_KEY);
		ClientSession.instance().resetPersistedPropsCache();
		return res;
	}
	
	public static void updatePersistedProp(final String key, final String value)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		bean.updatePersistedProp(key, value);
		ClientSession.instance().resetPersistedPropsCache();
	}
	
	public static void auditEvents(final AsyncLoadData<AuditEvent> provider, final UISynchronize sync, final AuditEventType type, 
			final Integer userId, final Integer gestiuneId, final LocalDate from, final LocalDate to, final String descriere, final Logger log)
	{
		final Job job = Job.create("Loading Audit Events", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<AuditEvent> auditEvents = bean.auditEvents(type, userId, gestiuneId, from, to, descriere);
				sync.asyncExec(() -> provider.success(auditEvents));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
	}
	
	public static Job contaDocs(final AsyncLoadData<AccountingDocument> provider, final UISynchronize sync, final Integer gestiuneId,
			final Long partnerId, final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Conta Docs", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<AccountingDocument> docs = bean.contaDocs(gestiuneId, partnerId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(docs));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static InvocationResult setEditableToAllContaDocs(final ImmutableSet<Long> accDocIds, final boolean editable)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.setEditableToAllContaDocs(accDocIds, editable);
	}
	
	public static Job rulajeParteneriInConta(final AsyncLoadData<RulajPartener> provider, final UISynchronize sync, final Integer gestiuneId,
			final Long partnerId, final LocalDate from, final LocalDate to, final Logger log)
	{
		final Job job = Job.create("Loading Rulaje Parteneri In Conta", (ICoreRunnable) monitor ->
		{
			try
			{
				final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
				final ImmutableList<RulajPartener> rulaje = bean.rulajeParteneriInConta(gestiuneId, partnerId, from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(rulaje));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job unverifiedOperations(final AsyncLoadData<Operatiune> provider, final UISynchronize sync, final Logger log)
	{
		final Job job = Job.create("Loading Unverified Operations", (ICoreRunnable) monitor ->
		{
			try
			{
				final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
				final ImmutableList<Operatiune> ops = bean.unverifiedOperations();
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(ops));
			}
			catch (final EJBException e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error("Eroare de server!"));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static ImmutableList<Operatiune> verifyBirthdays()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.verifyBirthdays();
	}
	
	public static ImmutableList<Operatiune> scheduleNextContractWork()
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.scheduleNextContractWork();
	}
	
	public static InvocationResult verifyOperation(final long operationId, final BigDecimal verificatCantitate)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.verifyOperation(operationId, verificatCantitate);
	}
	
	public static InvocationResult verifyOperationsInBulk(final ImmutableSet<Long> operationIds)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.verifyOperationsInBulk(operationIds);
	}
	
	public static void toggleHideUnofficialDocs()
	{
		final UsersBeanRemote bean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		bean.toggleHideUnofficialDocs();
	}
	
	public static String importJobs(final Logger log)
	{
		try
		{
			final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
			return bean.importJobs();
		}
		catch (final Exception e)
		{
			log.error(e);
			return e.getMessage();
		}
	}
	
	public static void importPartners(final ImmutableSet<Partner> partners)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importPartners(partners);
	}
	
	public static void importMesteri(final ImmutableSet<Partner> mesteri)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importMesteri(mesteri);
	}
	
	public static void importAccDocs(final ImmutableList<AccountingDocument> accDocs)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importAccDocs(accDocs);
	}
	
	public static void importDiscountDocs(final ImmutableSet<DocumentWithDiscount> discDocs)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importClientDocs(discDocs);
	}
	
	public static void importProducts(final ImmutableSet<Product> products)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importProducts(products);
	}
	
	public static void importOperations(final ImmutableSet<Operatiune> operations)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		bean.importOperations(operations);
	}
	
	public static InvocationResult replaceUserWith(final int userToDeleteId, final int replacedWithUserId)
	{
		final ImportBeanRemote bean = ServiceLocator.getBusinessService(ImportBean.class, ImportBeanRemote.class);
		return bean.deleteUser(userToDeleteId, replacedWithUserId);
	}
	
	public static Job profitability(final AsyncLoadResult<ImmutableMap<RaionProfitability, List<ProductProfitability>>> provider,
			final UISynchronize sync, final LocalDate from, final LocalDate until, final Gestiune gestiune, final ImmutableSet<String> docTypesToLoad)
	{
		final Job job = Job.create("Loading Profitability", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final ImmutableMap<RaionProfitability, List<ProductProfitability>> data = bean.profitability(from, until, gestiune, docTypesToLoad);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(data));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job productTurnover(final AsyncLoadResult<ImmutableList<ProductTurnover>> provider,
			final UISynchronize sync, final LocalDate from, final LocalDate until, final Gestiune gestiune, final ImmutableSet<String> docTypesToLoad)
	{
		final Job job = Job.create("Loading productTurnover", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final ImmutableList<ProductTurnover> data = bean.productTurnover(from, until, gestiune, docTypesToLoad);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(data));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job salesPerHours(final AsyncLoadResult<SalesPerHours> provider, final UISynchronize sync, final Gestiune gestiune,
			final ImmutableSet<String> docs)
	{
		final Job job = Job.create("Loading SalesPerHours", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final SalesPerHours result = bean.salesPerHours(gestiune, docs);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job salesPerOperators(final AsyncLoadResult<SalesPerOperators> provider, final UISynchronize sync,
			final Gestiune gestiune, final ImmutableSet<String> docs)
	{
		final Job job = Job.create("Loading SalesPerOperators", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final SalesPerOperators result = bean.salesPerOperators(gestiune, docs);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job lastYearStats(final AsyncLoadResult<LastYearStats> provider, final UISynchronize sync, final Gestiune gestiune)
	{
		final Job job = Job.create("Loading LastYearStats", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final LastYearStats result = bean.lastYearStats(gestiune);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static Job undeliveredDocs(final AsyncLoadData<AccountingDocument> provider, final UISynchronize sync, final LocalDate from, final LocalDate to)
	{
		final Job job = Job.create("Loading Undelivered Docs", (ICoreRunnable) monitor ->
		{
			final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
			final ImmutableList<AccountingDocument> docs = bean.undeliveredDocs(from, to);
			if (!monitor.isCanceled())
				sync.asyncExec(() -> provider.success(docs));
		});

		job.schedule();
		return job;
	}
	
	public static InvocationResult scheduleDoc(final long docId, final Long masinaId, final LocalDateTime transportDateTime,
			final String indicatiiDoc, final String addressDoc, final String phone, final TransportType transportType,
			final boolean payAtDriver)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.scheduleDoc(docId, masinaId, transportDateTime, indicatiiDoc, addressDoc, phone, transportType, payAtDriver);
	}
	
	public static Job pointsPerCar(final AsyncLoadResult<String> provider, final UISynchronize sync, final LocalDate from, final LocalDate to)
	{
		final Job job = Job.create("Loading PointsPerCar", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final String result = bean.pointsPerCar(from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(result));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static void uiCategories(final AsyncLoadData<ProductUiCategory> provider, final UISynchronize sync, final boolean lazyLoad,
			final boolean allCategories, final Bundle bundle, final Logger log)
	{
		final Job job = Job.create("Loading All UI Categories", (ICoreRunnable) monitor ->
		{
			final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
			final ImmutableList<ProductUiCategory> data = bean.uiCategories(lazyLoad, allCategories);
			sync.asyncExec(() -> provider.success(data));
			data.forEach(cat ->
			{
				if (lazyLoad)
					fillImageCache(bundle, log, cat.getProducts().stream()
							.map(Product::getImageUUID)
							.filter(Objects::nonNull)
							.collect(toImmutableSet()));
			});
		});

		job.schedule();
	}

	public static ImmutableList<ProductUiCategory> uiCategories_Sync(final Bundle bundle, final Logger log)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.uiCategories(false, true);
	}

	public static InvocationResult updateCategories(final ImmutableList<ProductUiCategory> newCategories)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateCategories(newCategories);
	}
	
	public static ImmutableList<CasaDepartment> casaDepts_Sync()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.casaDepartments();
	}
	
	public static InvocationResult updateCasaDepts(final ImmutableList<CasaDepartment> newDepts)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateCasaDepartments(newDepts);
	}
	
	public static ImmutableList<Raion> raioane_Sync()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.raioane();
	}
	
	public static InvocationResult updateRaioane(final ImmutableList<Raion> newRaioane)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateRaioane(newRaioane);
	}
	
	public static ImmutableList<Product> discountProducts()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.discountProducts();
	}
	
	public static ImmutableList<Reducere> reduceri_Sync()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.reduceri();
	}
	
	public static InvocationResult updateReduceri(final ImmutableList<Reducere> newReduceri)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateReduceri(newReduceri);
	}
	
	public static ImmutableList<GrupaInteres> grupeInteres_Sync()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.grupeInteres();
	}
	
	public static InvocationResult updateGrupeInteres(final ImmutableList<GrupaInteres> newGrupe)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateGrupeInteres(newGrupe);
	}
	
	public static long firstFreeNumber(final Class<? extends Serializable> entity, final String field)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.firstFreeNumber(entity, field);
	}
	
	public static Future<InvocationResult> sendMail(final String from, final String addresses, final String subject, final String htmlMessage,
			final byte[] fileAttachement, final String... bcc)
	{
		final MailSenderRemote bean = ServiceLocator.getBusinessService(MailSender.class, MailSenderRemote.class);
		return bean.send(from, addresses, subject, htmlMessage, fileAttachement, bcc);
	}
	
	public static InvocationResult comaseazaOpsFromDoc(final long docId)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.comaseazaOpsFromDoc(docId);
	}
	
	public static ImmutableList<ContBancar> allConturiBancare()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.allConturiBancare();
	}
	
	public static InvocationResult persistContBancar(final String name, final String iban, final String banca, final String valuta)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.persistContBancar(name, iban, banca, valuta);
	}
	
	public static InvocationResult updateContBancar(final int id, final String name, final String iban, final String banca, final String valuta)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateContBancar(id, name, iban, banca, valuta);
	}
	
	public static ImmutableList<Company> allCompanies()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.allCompanies();
	}
	
	public static Job clasament(final AsyncLoadData<ClasamentEntry> provider, final UISynchronize sync, final Logger log,
			final LocalDate from, final LocalDate to)
	{
		final Job job = Job.create("Loading Filtered Docs", (ICoreRunnable) monitor ->
		{
			try
			{
				final StatisticsBeanRemote bean = ServiceLocator.getBusinessService(StatisticsBean.class, StatisticsBeanRemote.class);
				final ImmutableList<ClasamentEntry> clasament = bean.clasament(from, to);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(clasament));
			}
			catch (final Exception e)
			{
				log.error(e);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static InvocationResult persistRecenzie(final Recenzie recenzie)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.persistRecenzie(recenzie);
	}
	
	public static InvocationResult fixStocuriAltaCompanie()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.fixStocuriAltaCompanie();
	}
	
	public static Job pontaje(final AsyncLoadData<PontajLine> provider,
			final UISynchronize sync, final Gestiune gestiune, final LocalDate month)
	{
		final Job job = Job.create("Loading Pontaje", (ICoreRunnable) monitor ->
		{
			try
			{
				final UsersBeanRemote bean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
				final ImmutableList<PontajLine> data = bean.pontaje(gestiune, month);
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.success(data));
			}
			catch (final Exception e)
			{
				if (!monitor.isCanceled())
					sync.asyncExec(() -> provider.error(e.getMessage()));
			}
		});

		job.schedule();
		return job;
	}
	
	public static InvocationResult mergePontaje(final ImmutableSet<PontajZilnic> pontaje)
	{
		if (pontaje.isEmpty())
			return InvocationResult.ok();
		
		final UsersBeanRemote bean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return bean.mergePontaje(pontaje);
	}
	
	public static InvocationResult updateProductFurnizoriToLatest()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateProductFurnizoriToLatest();
	}
	
	public static InvocationResult mergeImageLobs(final ImmutableList<LobImage> images)
	{
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		final ImmutableList<LobImage> filteredImages = images.stream()
				.filter(img -> img.getId() != null || img.getImage() != null)
				.collect(toImmutableList());
		if (filteredImages.isEmpty())
			return InvocationResult.ok();
		return bean.mergeImageLobs(filteredImages);
	}
	
	public static byte[] imageFromUuid(final Bundle bundle, final Logger log, final String uuid, final boolean lazyLoad)
	{
		if (isEmpty(uuid))
			return null;
		
		final String path = productImageFilename(uuid);
		if (Icons.imgRelativePathExists(bundle, log, path))
			try
			{
				return Icons.imgRelativePathToBytes(bundle, path);
			}
			catch (final IOException ex)
			{
				log.error(ex);
			}
		
		if (!lazyLoad)
			return null;
		
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		final byte[] foundImage = bean.getImageLobs(uuid).stream()
				.findFirst()
				.map(LobImage::getImage)
				.orElse(null);
		
		if (foundImage != null)
			try
			{
				Icons.saveImageBytesToRelativeFile(bundle, path, foundImage);
			}
			catch (final Exception e)
			{
				log.error(e);
			}
		
		return foundImage;
	}
	
	public static String imagePathFromUuid(final Bundle bundle, final Logger log, final String uuid)
	{
		if (isEmpty(uuid))
			return null;
		
		final String path = productImageFilename(uuid);
		if (Icons.imgRelativePathExists(bundle, log, path))
				return path;
		
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		final byte[] foundImage = bean.getImageLobs(uuid).stream()
				.findFirst()
				.map(LobImage::getImage)
				.orElse(null);
		
		if (foundImage != null)
			try
			{
				Icons.saveImageBytesToRelativeFile(bundle, path, foundImage);
			}
			catch (final Exception e)
			{
				log.error(e);
			}
		
		return path;
	}
	
	public static InvocationResult billImages()
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.billImages();
	}
	
	public static InvocationResult updateBillImages(final String signatureProp, final String stampProp)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.updateBillImages(signatureProp, stampProp);
	}
	
	public static void incasariOfDrivers(final AsyncLoadData<TempDocument> provider, final UISynchronize sync)
	{
		final Job job = Job.create("Loading Incasari of Drivers", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final ImmutableList<TempDocument> data = bean.incasariOfDrivers();
			if (!monitor.isCanceled())
				sync.asyncExec(() -> provider.success(data));
		});
	
		job.schedule();
	}
	
	public static InvocationResult deleteTempDocs(final ImmutableSet<Long> tempDocIds)
	{
		if (tempDocIds.isEmpty())
			return InvocationResult.ok();
	
		final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
		return bean.deleteTempDocs(tempDocIds);
	}
	
	public static Optional<CIM> cimForUser(final int userId)
	{
		final UsersBeanRemote bean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return Optional.ofNullable(bean.cimForUser(userId));
	}
	
	public static InvocationResult mergeCim(final CIM cim, final Integer userId)
	{
		final UsersBeanRemote bean = ServiceLocator.getBusinessService(UsersBean.class, UsersBeanRemote.class);
		return bean.mergeCim(cim, userId);
	}
	
	public static ImmutableList<AccountingDocument> salariiForMonth(final Gestiune gestiune, final LocalDate month)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.salariiForMonth(gestiune, month);
	}
	
	public static ImmutableList<AccountingDocument> calculateTicheteMasa(final Gestiune gestiune, final LocalDate month)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.calculateTicheteMasa(gestiune, month);
	}
	
	public static ImmutableList<AccountingDocument> calculateSalarii(final Gestiune gestiune, final LocalDate month)
	{
		final ManagerBeanRemote bean = ServiceLocator.getBusinessService(ManagerBean.class, ManagerBeanRemote.class);
		return bean.calculateSalarii(gestiune, month);
	}
	
	public static Job loadUsedAddresses(final AsyncLoadData<ThreeEntityWrapper<String>> provider, final Partner partner)
	{
		final Job job = Job.create("Loading last used addresses", (ICoreRunnable) monitor ->
		{
			final VanzariBeanRemote bean = ServiceLocator.getBusinessService(VanzariBean.class, VanzariBeanRemote.class);
			final ImmutableList<ThreeEntityWrapper<String>> data = bean.lastUsedAddresses(partner);
			if (!monitor.isCanceled())
				provider.success(data);
		});
	
		if (partner != null)
			job.schedule();
		return job;
	}
}
