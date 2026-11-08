import { dateInTimeZone } from '../../domain/note/due-date.js';
import type { RecentTasks } from '../../domain/task/task-summary.js';
import type { Logger } from '../ports/logger.js';
import type { TaskGateway } from '../ports/task-gateway.js';

/** Margen para que, tras ocultar las vencidas, sigan quedando tareas recientes. */
const FETCH_LIMIT = 100;

/**
 * Historial del reloj: tareas recientes del listado principal.
 *
 * Las vencidas no se borran de Google Tasks; solo se ocultan aquí. Se comparan como cadenas
 * `AAAA-MM-DD`, que ordenan igual que las fechas. El «hoy» se calcula en la zona configurada para
 * que el reloj y el backend etiqueten los días igual.
 */
export class ListRecentTasksUseCase {
  constructor(
    private readonly tasks: TaskGateway,
    private readonly timeZone: string,
    private readonly clock: () => Date = () => new Date(),
  ) {}

  async execute(limit: number, logger: Logger): Promise<RecentTasks> {
    const today = dateInTimeZone(this.clock(), this.timeZone);
    const fetched = await this.tasks.list(FETCH_LIMIT);
    const visible = fetched.filter((task) => task.dueDate === null || task.dueDate >= today);

    // Nunca se registran títulos (RN-07, RNF-18): solo cantidades.
    logger.info(
      { fetched: fetched.length, visible: visible.length, hidden: fetched.length - visible.length },
      'Recent tasks listed',
    );

    return { today, tasks: visible.slice(0, limit) };
  }
}
