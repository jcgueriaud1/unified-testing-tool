package com.vaadin.testing.unified.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.testing.unified.Findable;
import com.vaadin.testing.unified.UnifiedComponent;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.UnifiedWrapper;
import com.vaadin.testing.unified.component.UnifiedGenericComponent;
import com.vaadin.testing.unified.query.ComponentQuery;
import com.vaadin.testing.unified.query.Query;
import com.vaadin.testing.unified.spi.Filter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Implements wrapper and query interfaces at runtime (ADR 0003). */
public final class Wrappers {
    private Wrappers() {}

    // ---------------------------------------------------------------- entry points

    public static <Q> Q newQuery(TestRun run, Class<? extends Findable<Q>> wrapperType, LazyElement parent) {
        UnifiedComponent annotation = wrapperType.getAnnotation(UnifiedComponent.class);
        if (annotation == null) {
            throw new IllegalArgumentException(wrapperType.getName() + " has no @UnifiedComponent annotation");
        }
        Class<?> queryType = typeArgument(wrapperType, Findable.class, 0);
        ElementSource source = new ElementSource(run, wrapperType.getSimpleName(), annotation.value(),
                tagOf(annotation.value(), annotation.tag()), List.of(), List.of(), List.of(), parent);
        @SuppressWarnings("unchecked")
        Q query = (Q) newQueryProxy(queryType, source);
        return query;
    }

    public static ComponentQuery newComponentQuery(TestRun run, Class<? extends Component> componentType,
            LazyElement parent) {
        ElementSource source = new ElementSource(run, "findComponent(" + componentType.getSimpleName() + ")",
                componentType, tagOf(componentType, ""), List.of(), List.of(), List.of(), parent);
        return (ComponentQuery) newQueryProxy(ComponentQuery.class, source);
    }

    /** The lazy element behind a wrapper created by this class. */
    public static LazyElement lazyElementOf(UnifiedWrapper wrapper) {
        if (Proxy.isProxyClass(wrapper.getClass())
                && Proxy.getInvocationHandler(wrapper) instanceof WrapperHandler handler) {
            return handler.element;
        }
        if (wrapper.element() instanceof LazyElement lazy) {
            return lazy;
        }
        throw new IllegalArgumentException("Not a Unified API wrapper: " + wrapper);
    }

    static String tagOf(Class<? extends Component> componentType, String explicitTag) {
        if (!explicitTag.isEmpty()) {
            return explicitTag;
        }
        Tag tag = componentType.getAnnotation(Tag.class);
        return tag == null ? null : tag.value();
    }

    // ---------------------------------------------------------------- queries

    private static Object newQueryProxy(Class<?> queryType, ElementSource source) {
        Class<?> wrapperType = typeArgument(queryType, Query.class, 0);
        return Proxy.newProxyInstance(queryType.getClassLoader(), new Class<?>[] { queryType },
                new QueryHandler(queryType, wrapperType, source));
    }

    private record QueryHandler(Class<?> queryType, Class<?> wrapperType, ElementSource source)
            implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "single":
                    return newWrapper(wrapperType, new LazyElement(source, -1, source.run()));
                case "all": {
                    int count = source.resolve().size();
                    List<Object> wrappers = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        wrappers.add(newWrapper(wrapperType, new LazyElement(source, i, source.run())));
                    }
                    return List.copyOf(wrappers);
                }
                case "exists":
                    return !source.resolve().isEmpty();
                case "toString":
                    return source.describe();
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            ElementSource next = switch (name) {
                case "withLabel" -> source.with(new Filter(Filter.Kind.LABEL, null, (String) args[0], true));
                case "withLabelContaining" -> source.with(new Filter(Filter.Kind.LABEL, null, (String) args[0], false));
                case "withAccessibleName" -> source.with(new Filter(Filter.Kind.ACCESSIBLE_NAME, null, (String) args[0], true));
                case "withAccessibleNameContaining" -> source.with(new Filter(Filter.Kind.ACCESSIBLE_NAME, null, (String) args[0], false));
                case "withText" -> source.with(new Filter(Filter.Kind.TEXT, null, (String) args[0], true));
                case "withTextContaining" -> source.with(new Filter(Filter.Kind.TEXT, null, (String) args[0], false));
                case "withPlaceholder" -> source.with(new Filter(Filter.Kind.PLACEHOLDER, null, (String) args[0], true));
                case "withTestId" -> source.with(new Filter(Filter.Kind.TEST_ID, null, (String) args[0], true));
                case "withId" -> source.with(new Filter(Filter.Kind.ID, null, (String) args[0], true));
                case "withClassName" -> source.with(new Filter(Filter.Kind.CLASS_NAME, null, (String) args[0], true));
                case "withTheme" -> source.with(new Filter(Filter.Kind.THEME, null, (String) args[0], true));
                case "withAttribute" -> source.with(new Filter(Filter.Kind.ATTRIBUTE, (String) args[0], (String) args[1], true));
                case "withProperty" -> {
                    String property = (String) args[0];
                    Object expected = args[1];
                    yield source.with(e -> Objects.equals(String.valueOf(expected), e.getProperty(property)),
                            "withProperty(\"" + property + "\", " + expected + ")");
                }
                case "where" -> {
                    @SuppressWarnings("unchecked")
                    Predicate<UnifiedElement> condition = (Predicate<UnifiedElement>) args[0];
                    yield source.with(condition, "where(..)");
                }
                default -> throw new UnsupportedOperationException(
                        queryType.getSimpleName() + "." + name + " is not implemented");
            };
            return newQueryProxy(queryType, next);
        }
    }

    // ---------------------------------------------------------------- wrappers

    static Object newWrapper(Class<?> wrapperType, LazyElement element) {
        return Proxy.newProxyInstance(wrapperType.getClassLoader(), new Class<?>[] { wrapperType },
                new WrapperHandler(wrapperType, element));
    }

    private record WrapperHandler(Class<?> wrapperType, LazyElement element) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "element":
                    return element;
                case "unwrap":
                    return element.unwrap((Class<?>) args[0]);
                case "tryUnwrap":
                    return element.tryUnwrap((Class<?>) args[0]);
                case "withTimeout":
                    return newWrapper(wrapperType, element.withRun(element.run().withTimeout((Duration) args[0])));
                case "find":
                    if (args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Object query = newQuery(element.run(), (Class) type, element);
                        return query;
                    }
                    break;
                case "findComponent":
                    if (args != null && args.length == 1 && args[0] instanceof Class<?> type) {
                        @SuppressWarnings("unchecked")
                        Class<? extends Component> componentType = (Class<? extends Component>) type;
                        return newComponentQuery(element.run(), componentType, element);
                    }
                    break;
                case "toString":
                    return wrapperType.getSimpleName() + "[" + element.describe() + "]";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            if (method.isDefault()) {
                try {
                    return InvocationHandler.invokeDefault(proxy, method, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
            throw new UnsupportedOperationException(wrapperType.getSimpleName() + "." + method.getName()
                    + " has no default implementation (the wrapper must implement it on element())");
        }
    }

    // ---------------------------------------------------------------- generics

    /** The {@code index}-th type argument with which {@code type} implements {@code generic}. */
    static Class<?> typeArgument(Class<?> type, Class<?> generic, int index) {
        for (Type candidate : type.getGenericInterfaces()) {
            if (candidate instanceof ParameterizedType p && p.getRawType() == generic) {
                return raw(p.getActualTypeArguments()[index]);
            }
            Class<?> parent = raw(candidate);
            if (parent != null && generic.isAssignableFrom(parent) && parent != generic) {
                Class<?> found = typeArgument(parent, generic, index);
                if (found != null) {
                    return found;
                }
            }
        }
        if (type == ComponentQuery.class && generic == Query.class) {
            return UnifiedGenericComponent.class;
        }
        throw new IllegalArgumentException(type.getName() + " does not declare " + generic.getSimpleName() + "<..>");
    }

    private static Class<?> raw(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType p) {
            return (Class<?>) p.getRawType();
        }
        return null;
    }
}
