package ro.linic.ui.base.services.nattable.internal;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.nebula.widgets.nattable.command.ILayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.datachange.DataChangeLayer;
import org.eclipse.nebula.widgets.nattable.datachange.UpdateDataChange;
import org.eclipse.nebula.widgets.nattable.datachange.UpdateDataChangeHandler;
import org.eclipse.nebula.widgets.nattable.datachange.command.SaveDataChangesCommand;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;

import ro.linic.ui.base.services.mapper.UpdateCommandMapper;
import ro.linic.ui.base.services.nattable.UpdateCommand;

public class SaveDataChangesDelegateCommandHandler<T> implements ILayerCommandHandler<SaveDataChangesCommand> {
	private final IColumnPropertyAccessor<T> cpa;
    private final DataChangeLayer dataChangeLayer;
	private final Class<T> modelClass;
	private final MDirtyable dirtyable;
	private final Function<List<UpdateCommand>, Boolean> saver;

	/**
	 * @param dirtyable can be null
	 */
	public SaveDataChangesDelegateCommandHandler(final IColumnPropertyAccessor<T> columnPropertyAccessor,
			final DataChangeLayer dataChangeLayer, final FluentTableConfigurer<T> configurer) {
		this.cpa = columnPropertyAccessor;
		this.dataChangeLayer = dataChangeLayer;
		this.modelClass = configurer.getModelClass();
		this.dirtyable = configurer.getDirtyable();
		this.saver = configurer.getSaveToDbHandler();
	}

	@Override
	final public boolean doCommand(final ILayer targetLayer, final SaveDataChangesCommand command) {
		if (saver != null) {
			@SuppressWarnings("unchecked")
			final List<UpdateCommand> commands = this.dataChangeLayer.getDataChangeHandler().stream()
					.filter(UpdateDataChangeHandler.class::isInstance)
					.map(UpdateDataChangeHandler.class::cast)
					.flatMap(h -> (Stream<UpdateDataChange>) h.getDataChanges().values().stream())
					.map(udc -> UpdateCommandMapper.INSTANCE.from(modelClass, cpa, udc))
					.collect(Collectors.toList());
			
			if (saver.apply(commands))
				this.dataChangeLayer.saveDataChanges();
		} else
			this.dataChangeLayer.saveDataChanges();
		
		if (dirtyable != null)
			dirtyable.setDirty(this.dataChangeLayer.isDirty());
		return true;
	}

	@Override
    public Class<SaveDataChangesCommand> getCommandClass() {
        return SaveDataChangesCommand.class;
    }
}
