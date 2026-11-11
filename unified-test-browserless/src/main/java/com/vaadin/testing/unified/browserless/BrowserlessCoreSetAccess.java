package com.vaadin.testing.unified.browserless;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsTester;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsTester;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadTester;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.testing.unified.UnifiedElement;
import com.vaadin.testing.unified.spi.DateTimePickerAccess;
import com.vaadin.testing.unified.spi.DetailsAccess;
import com.vaadin.testing.unified.spi.MenuBarAccess;
import com.vaadin.testing.unified.spi.TabSheetAccess;
import com.vaadin.testing.unified.spi.UploadAccess;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Access to Tabs, Details, MenuBar, Upload and DateTimePicker on Browserless, through their testers. */
final class BrowserlessCoreSetAccess {
    private BrowserlessCoreSetAccess() {}

    /** A bare Tabs bar: TabSheetAccess without content. Tabs are found by their Item Name. */
    record TabsAccess(BrowserlessSession session, Tabs bar) implements TabSheetAccess {
        private List<Tab> all() {
            return bar.getChildren().filter(Tab.class::isInstance).map(Tab.class::cast).toList();
        }

        @Override
        public List<String> tabs() {
            return all().stream().map(ItemNames::of).toList();
        }

        @Override
        public String selectedTab() {
            Tab selected = bar.getSelectedTab();
            return selected == null ? null : ItemNames.of(selected);
        }

        @Override
        public boolean isEnabled(String label) {
            return ItemNames.find(all(), label, "tab \"" + label + "\"").isEnabled();
        }

        /** By position: TabsTester.select(label) compares the label, which a tab holding a component lacks. */
        @Override
        public void select(String label) {
            session.act(() -> {
                List<Tab> all = all();
                int position = ItemNames.visiblePosition(all, ItemNames.find(all, label, "tab \"" + label + "\""), "Tab");
                ((TabsTester<?>) session.window().test(bar)).select(position);
                session.window().roundTrip();
            });
        }

        @Override
        public UnifiedElement content() {
            throw new UnsupportedOperationException("A Tabs bar has no content; use UnifiedTabSheet for a TabSheet");
        }
    }

    record DetailsAccessImpl(BrowserlessSession session, Details details) implements DetailsAccess {
        @Override
        public String summaryText() {
            return details.getSummaryText();
        }

        @Override
        public boolean isOpen() {
            return details.isOpened();
        }

        @Override
        public void toggle() {
            session.act(() -> {
                ((DetailsTester<?>) session.window().test(details)).toggleDetails();
                session.window().roundTrip();
            });
        }

        @Override
        public UnifiedElement content() {
            List<Component> content = details.getContent().toList();
            return new BrowserlessElement(session, details, content);
        }
    }

    /**
     * Items found by their Item Name, then clicked by position: MenuBarTester matches an item's own text,
     * which an icon-only item lacks.
     */
    record MenuBarAccessImpl(BrowserlessSession session, MenuBar menuBar) implements MenuBarAccess {

        @Override
        public List<String> items(List<String> path) {
            List<MenuItem> items = path.isEmpty() ? menuBar.getItems()
                    : resolve(path).get(path.size() - 1).getSubMenu().getItems();
            return items.stream().filter(Component::isVisible).map(ItemNames::of).toList();
        }

        @Override
        public void click(List<String> path) {
            session.act(() -> {
                List<MenuItem> chosen = resolve(path);
                int[] positions = new int[path.size()];
                List<MenuItem> level = menuBar.getItems();
                for (int i = 0; i < path.size(); i++) {
                    positions[i] = ItemNames.visiblePosition(level, chosen.get(i), "Menu item");
                    level = chosen.get(i).getSubMenu().getItems();
                }
                ((MenuBarTester<?>) session.window().test(menuBar)).clickItem(positions[0],
                        java.util.Arrays.copyOfRange(positions, 1, positions.length));
                session.window().roundTrip();
            });
        }

        @Override
        public boolean isEnabled(List<String> path) {
            return resolve(path).get(path.size() - 1).isEnabled();
        }

        /** The item of each level of the path. */
        private List<MenuItem> resolve(List<String> path) {
            List<MenuItem> chosen = new java.util.ArrayList<>();
            List<MenuItem> level = menuBar.getItems();
            for (int i = 0; i < path.size(); i++) {
                MenuItem item = ItemNames.find(level, path.get(i), "menu item \"" + path.get(i) + "\" at " + path.subList(0, i + 1));
                chosen.add(item);
                level = item.getSubMenu().getItems();
            }
            return chosen;
        }
    }

    /**
     * Typing into a DateTimePicker's halves, as a browser reports it to the server: each half is a date or
     * time picker child of the field, set from the client when its text parses (ISO only), else given the
     * text as input and an {@code unparsable-change}; then the field gets the value, or an
     * {@code unparsable-change} that makes it validate the halves (bad input, incomplete). Builds on Flow's
     * internals: Browserless Test can't type into a DateTimePicker (#100).
     */
    record DateTimePickerAccessImpl(BrowserlessSession session, DateTimePicker picker) implements DateTimePickerAccess {
        @Override
        public void typeText(String date, String time) {
            session.act(() -> {
                BrowserlessUIContext window = session.window();
                Optional<LocalDate> parsedDate = parse(date, LocalDate::parse);
                Optional<LocalTime> parsedTime = parse(time, LocalTime::parse);
                type(window, half(picker, DatePicker.class), date, parsedDate, LocalDate.class);
                type(window, half(picker, TimePicker.class), time, parsedTime, LocalTime.class);
                if (parsedDate.isPresent() && parsedTime.isPresent()) {
                    tester(window, picker, "setValue", LocalDateTime.class,
                            LocalDateTime.of(parsedDate.get(), parsedTime.get()));
                } else {
                    if (!picker.isEmpty()) {
                        tester(window, picker, "setValue", LocalDateTime.class, null);
                    }
                    if (!date.isBlank() || !time.isBlank()) {
                        com.vaadin.browserless.internal.BasicUtilsKt._fireDomEvent(picker, "unparsable-change");
                    }
                }
                window.roundTrip();
            });
        }

        /**
         * Sets the value as a user entering it does: both halves from the client, then the field. Browserless
         * Test's DateTimePickerTester sets the field only, so a half kept text it couldn't parse.
         */
        static void setValueAsUser(BrowserlessUIContext window, DateTimePicker picker, LocalDateTime value) {
            setHalf(window, half(picker, DatePicker.class), value == null ? null : value.toLocalDate(), LocalDate.class);
            setHalf(window, half(picker, TimePicker.class), value == null ? null : value.toLocalTime(), LocalTime.class);
            tester(window, picker, "setValue", LocalDateTime.class, value);
        }

        private static void type(BrowserlessUIContext window, Component half, String text, Optional<?> parsed,
                Class<?> type) {
            if (parsed.isPresent() || text.isBlank()) {
                setHalf(window, half, parsed.orElse(null), type);
            } else {
                half.getElement().setProperty("_inputElementValue", text);
                com.vaadin.browserless.internal.BasicUtilsKt._fireDomEvent(half, "unparsable-change");
            }
        }

        /** A half's value from the client; its input text is no longer what it couldn't parse. */
        private static void setHalf(BrowserlessUIContext window, Component half, Object value, Class<?> type) {
            half.getElement().setProperty("_inputElementValue", "");
            if (value == null) {
                tester(window, half, "clear", null, null);
            } else {
                tester(window, half, "setValue", type, value);
            }
        }

        private static Component half(DateTimePicker picker, Class<? extends Component> type) {
            return picker.getChildren().filter(type::isInstance).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No " + type.getSimpleName() + " in the DateTimePicker"));
        }

        private static <T> Optional<T> parse(String text, java.util.function.Function<String, T> parser) {
            try {
                return text.isBlank() ? Optional.empty() : Optional.of(parser.apply(text.strip()));
            } catch (java.time.format.DateTimeParseException e) {
                return Optional.empty();
            }
        }

        /** Calls the tester's method: with no argument when {@code type} is null. */
        private static void tester(BrowserlessUIContext window, Component component, String method, Class<?> type,
                Object value) {
            Object tester = window.test(component);
            try {
                if (type == null) {
                    tester.getClass().getMethod(method).invoke(tester);
                } else {
                    tester.getClass().getMethod(method, type).invoke(tester, value);
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    record UploadAccessImpl(BrowserlessSession session, Upload upload) implements UploadAccess {
        @Override
        public void upload(String fileName, String mimeType, byte[] content) {
            session.act(() -> {
                ((UploadTester<?>) session.window().test(upload)).upload(fileName, mimeType, content);
                session.window().roundTrip();
            });
        }
    }
}
