package ru.tpu.hostel.internal.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Роли юзеров. Содержит статические методы для проверки прав роли на те или иные действия
 *
 * @author Кудрявцев Глеб, Лапшин Илья
 * @version 1.0.0
 * @since 1.0.0
 */
@RequiredArgsConstructor
public enum Roles {

    ADMINISTRATION("Администрация", null, null, null),
    HOSTEL_SUPERVISOR("Староста общежития", ADMINISTRATION, null, null),
    FLOOR_SUPERVISOR("Староста этажа", HOSTEL_SUPERVISOR, null, null),
    RESPONSIBLE_KITCHEN("Ответственный за кухню", FLOOR_SUPERVISOR, ResourceType.KITCHEN, ResourceType.KITCHEN),
    RESPONSIBLE_HALL("Ответственный за зал", HOSTEL_SUPERVISOR, ResourceType.HALL, ResourceType.HALL),
    RESPONSIBLE_GYM("Главный за спортзал", HOSTEL_SUPERVISOR, ResourceType.GYM, ResourceType.GYM),
    WORKER_GYM("Ответственный за спортзал", RESPONSIBLE_GYM, null, ResourceType.GYM),
    RESPONSIBLE_FIRE_SAFETY("Ответственный за пожарную безопасность", HOSTEL_SUPERVISOR, null, null),
    RESPONSIBLE_SANITARY("Ответственный за санитарную проверку", FLOOR_SUPERVISOR, null, null),
    RESPONSIBLE_INTERNET("Ответственный за подключение к Интернету", HOSTEL_SUPERVISOR, ResourceType.INTERNET, ResourceType.INTERNET),
    RESPONSIBLE_SOOP("Ответственный за СООП", HOSTEL_SUPERVISOR, ResourceType.SOOP, ResourceType.SOOP),
    WORKER_FIRE_SAFETY("Работник пожарной безопасности", RESPONSIBLE_FIRE_SAFETY, null, null),
    WORKER_SANITARY("Работник санитарной проверки", RESPONSIBLE_SANITARY, null, null),
    WORKER_SOOP("Работник СООП", RESPONSIBLE_SOOP, null, ResourceType.SOOP),
    STUDENT("Студент", null, null, null);

    /**
     * Маппа со всеми вложенными ролями для каждой роли
     */
    private static final Map<Roles, Set<Roles>> INHERITED_ROLES = new EnumMap<>(Roles.class);

    static {
        for (Roles role : values()) {
            INHERITED_ROLES.put(role, role.getAllInheritedRoles());
        }
    }

    @Getter
    private final String roleName;

    private final Roles parentRole;

    /**
     * Ресурс, которым может управлять роль
     */
    private final ResourceType managingResourceType;

    /**
     * Ресурс, на который может быть назначена роль в качестве ответственного/управляющего
     */
    private final ResourceType assignedResourceType;

    public static boolean isRoleHigherThan(Roles role1, Roles role2) {
        if (role1 == null || role2 == null) {
            return false;
        }
        return role1.getDepth() < role2.getDepth();
    }

    public static boolean isRoleHigherThan(Collection<Roles> roles1, Collection<Roles> roles2) {
        if (roles1 == null || roles2 == null || roles1.isEmpty() || roles2.isEmpty()) {
            return false;
        }

        Roles seniorRole1 = getSeniorRole(roles1);
        Roles seniorRole2 = getSeniorRole(roles2);
        return isRoleHigherThan(seniorRole1, seniorRole2);
    }

    public static boolean isRoleHigherThan(Roles[] roles1, Roles[] roles2) {
        return isRoleHigherThan(Arrays.asList(roles1), Arrays.asList(roles2));
    }

    /**
     * Проверят, может ли хоть одна из ролей коллекции быть назначена в качестве ответственного/управляющего ресурсом
     *
     * @param roles              коллекция ролей
     * @param targetResourceType ресурс
     * @return разрешение на назначение
     */
    public static boolean canBeAssignedToResourceType(Collection<Roles> roles, ResourceType targetResourceType) {
        if (roles == null
                || roles.isEmpty()
                || targetResourceType == null
                || roles.contains(Roles.ADMINISTRATION)
                || (!roles.contains(Roles.RESPONSIBLE_INTERNET) && targetResourceType.equals(ResourceType.INTERNET))) {
            return false;
        }

        Roles seniorRole = getSeniorRole(roles);
        if (seniorRole == null) {
            return false;
        }

        return seniorRole.canBeAssignedToResourceType(targetResourceType);
    }

    /**
     * Проверят, может ли хоть одна из ролей массива быть назначена в качестве ответственного/управляющего ресурсом
     *
     * @param roles              массив ролей
     * @param targetResourceType ресурс
     * @return разрешение на назначение
     */
    public static boolean canBeAssignedToResourceType(Roles[] roles, ResourceType targetResourceType) {
        return canBeAssignedToResourceType(Arrays.asList(roles), targetResourceType);
    }

    /**
     * Проверят, может ли хоть одна из ролей массива быть назначена в качестве ответственного/управляющего ресурсом
     *
     * @param roles              массив ролей
     * @param targetResourceType ресурс
     * @return разрешение на назначение
     */
    public static boolean canBeAssignedToResourceType(Roles[] roles, Enum<?> targetResourceType) {
        ResourceType resourceType = ResourceType.cast(targetResourceType);
        return canBeAssignedToResourceType(roles, resourceType);
    }

    /**
     * Проверят, может ли хоть одна из ролей коллекции быть назначена в качестве ответственного/управляющего ресурсом
     *
     * @param roles              коллекция ролей
     * @param targetResourceType ресурс
     * @return разрешение на назначение
     */
    public static boolean canBeAssignedToResourceType(Collection<Roles> roles, Enum<?> targetResourceType) {
        ResourceType resourceType = ResourceType.cast(targetResourceType);
        return canBeAssignedToResourceType(roles, resourceType);
    }

    /**
     * Возвращает все роли, от которых наследуется старшая из коллекции. Идем вниз по иерархии
     *
     * @param roles коллекция ролей
     * @return всех потомков ролей
     */
    public static Set<Roles> getAllInheritedRoles(Collection<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        Roles seniorRole = getSeniorRole(roles);
        if (seniorRole == null) {
            return Collections.emptySet();
        }
        return INHERITED_ROLES.get(seniorRole);
    }

    /**
     * Возвращает все роли, от которых наследуется старшая из массива. Идем вниз по иерархии
     *
     * @param roles массив ролей
     * @return всех потомков старшей роли
     */
    public static Set<Roles> getAllInheritedRoles(Roles[] roles) {
        return getAllInheritedRoles(Arrays.asList(roles));
    }

    /**
     * Возвращает все роли, которым подчиняется старшая из коллекции. Идем вверх по иерархии
     *
     * @param roles коллекция ролей
     * @return всех родителей старшей роли
     */
    public static Set<Roles> getAllParentRoles(Collection<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        Roles seniorRole = getSeniorRole(roles);
        if (seniorRole == null) {
            return Collections.emptySet();
        }
        return seniorRole.getAllParentRoles();
    }

    /**
     * Возвращает все роли, которым подчиняется старшая из массива. Идем вверх по иерархии
     *
     * @param roles массив ролей
     * @return всех родителей старшей роли
     */
    public static Set<Roles> getAllParentRoles(Roles[] roles) {
        return getAllParentRoles(Arrays.asList(roles));
    }

    /**
     * Проверяет, может ли хоть одна из ролей коллекции управлять другой (назначать, редактировать, удалять)
     *
     * @param roles      коллекция ролей
     * @param targetRole роль для управления
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageRole(Collection<Roles> roles, Roles targetRole) {
        if (roles == null || roles.isEmpty() || targetRole == null) {
            return false;
        }

        Roles seniorRole = getSeniorRole(roles);
        if (seniorRole == null) {
            return false;
        }
        return seniorRole.hasPermissionToManageRole(targetRole);
    }

    /**
     * Проверяет, может ли хоть одна из ролей массива управлять другой (назначать, редактировать, удалять)
     *
     * @param roles      массив ролей
     * @param targetRole роль для управления
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageRole(Roles[] roles, Roles targetRole) {
        return hasPermissionToManageRole(Arrays.asList(roles), targetRole);
    }

    /**
     * Проверяет, может ли хоть одна из ролей коллекции управлять ресурсом (редактировать, удалять)
     *
     * @param roles              коллекция ролей
     * @param targetResourceType ресурс
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageResourceType(Collection<Roles> roles, ResourceType targetResourceType) {
        if (roles == null || roles.isEmpty() || targetResourceType == null) {
            return false;
        }

        return roles.stream().anyMatch(role -> role.hasPermissionToManageResourceType(targetResourceType));
    }

    /**
     * Проверяет, может ли хоть одна из ролей массива управлять ресурсом (редактировать, удалять)
     *
     * @param roles              массив ролей
     * @param targetResourceType ресурс
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageResourceType(Roles[] roles, ResourceType targetResourceType) {
        return hasPermissionToManageResourceType(Arrays.asList(roles), targetResourceType);
    }

    /**
     * Проверяет, может ли хоть одна из ролей массива управлять ресурсом (редактировать, удалять)
     *
     * @param roles              массив ролей
     * @param targetResourceType ресурс
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageResourceType(Roles[] roles, Enum<?> targetResourceType) {
        ResourceType resourceType = ResourceType.cast(targetResourceType);
        return hasPermissionToManageResourceType(roles, resourceType);
    }

    /**
     * Проверяет, может ли хоть одна из ролей коллекции управлять ресурсом (редактировать, удалять)
     *
     * @param roles              коллекция ролей
     * @param targetResourceType ресурс
     * @return разрешение на управление
     */
    public static boolean hasPermissionToManageResourceType(Collection<Roles> roles, Enum<?> targetResourceType) {
        ResourceType resourceType = ResourceType.cast(targetResourceType);
        return hasPermissionToManageResourceType(roles, resourceType);
    }

    /**
     * Возвращает самую старшую роль из коллекции
     *
     * @param roles коллекция ролей
     * @return старшую роль
     */
    private static Roles getSeniorRole(Collection<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.stream()
                .min(Comparator.comparing(Roles::getDepth))
                .orElse(null);
    }

    /**
     * Возвращает самую старшую роль из массива
     *
     * @param roles массив ролей
     * @return старшую роль
     */
    private static Roles getSeniorRole(Roles[] roles) {
        return getSeniorRole(Arrays.asList(roles));
    }

    /**
     * Возвращает самую младшую роль из коллекции
     *
     * @param roles коллекция ролей
     * @return младшую роль
     */
    private static Roles getJuniorRole(Collection<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        return roles.stream()
                .max(Comparator.comparing(Roles::getDepth))
                .orElse(null);
    }

    /**
     * Возвращает самую младшую роль из массива
     *
     * @param roles массив ролей
     * @return младшую роль
     */
    private static Roles getJuniorRole(Roles[] roles) {
        return getJuniorRole(Arrays.asList(roles));
    }

    /**
     * Все роли, от которых наследуется текущая. Идем от текущей роли вниз по иерархии
     */
    private Set<Roles> getAllInheritedRoles() {
        if (this == STUDENT) {
            return Collections.emptySet();
        }

        Set<Roles> roles = new HashSet<>();
        roles.add(this);
        for (Roles role : values()) {
            if (role.parentRole == this) {
                roles.addAll(role.getAllInheritedRoles());
            }
        }

        return Collections.unmodifiableSet(roles);
    }

    /**
     * Все роли, которым подчиняется текущая. Идем от текущей роли вверх по иерархии
     */
    private Set<Roles> getAllParentRoles() {
        Set<Roles> roles = new HashSet<>();

        Roles currentRole = this;

        while (currentRole != null) {
            roles.add(currentRole);
            currentRole = currentRole.parentRole;
        }

        return Collections.unmodifiableSet(roles);
    }

    /**
     * Проверяет, может ли текущая роль управлять другой (назначать, редактировать, удалять)
     *
     * @param targetRole роль для управления
     * @return разрешение на управление
     */
    private boolean hasPermissionToManageRole(Roles targetRole) {
        if (targetRole == null) {
            return false;
        }
        return INHERITED_ROLES.get(this).contains(targetRole);
    }

    /**
     * Проверяет, может ли текущая роль управлять ресурсом (редактировать, удалять)
     *
     * @param targetResourceType ресурс
     * @return разрешение на управление
     */
    private boolean hasPermissionToManageResourceType(ResourceType targetResourceType) {
        if (targetResourceType == null) {
            return false;
        }
        return managingResourceType == targetResourceType;
    }

    /**
     * Проверят, может ли текущая роль быть назначена в качестве ответственного/управляющего ресурсом
     *
     * @param targetResourceType ресурс
     * @return разрешение на назначение
     */
    private boolean canBeAssignedToResourceType(ResourceType targetResourceType) {
        if (targetResourceType == null) {
            return false;
        }
        return getAllAssignedResourceTypes().contains(targetResourceType);
    }

    /**
     * Возвращает глубину роли от Администрации
     *
     * @return глубину. Для {@link #STUDENT} всегда {@link Integer#MAX_VALUE}
     */
    private int getDepth() {
        if (this == STUDENT) {
            return Integer.MAX_VALUE;
        }

        return getAllParentRoles().size();
    }

    /**
     * Возвращает все ресурсы, которыми может управлять роль
     *
     * @return сет ресурсов
     */
    private Set<ResourceType> getAllAssignedResourceTypes() {
        return INHERITED_ROLES.get(this).stream()
                .map(role -> role.assignedResourceType)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Перечисление ресурсов
     */
    @RequiredArgsConstructor
    @Getter
    public enum ResourceType {

        GYM("Спортивный зал"),
        HALL("Зал"),
        INTERNET("Интернет"),
        KITCHEN("Кухня"),
        SOOP("СООП");

        private final String resourceTypeName;

        /**
         * Кастит любой енам в ResourceType
         *
         * @param enumValue енам
         * @return элемент ResourceType
         */
        public static ResourceType cast(Enum<?> enumValue) {
            if (enumValue == null) {
                return null;
            }
            if (enumValue instanceof ResourceType resourceType) {
                return resourceType;
            }

            try {
                return valueOf(enumValue.name());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

}