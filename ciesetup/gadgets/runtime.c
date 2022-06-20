/*
 * ciesetup - The ultimate workarounds to fix an ancient game
 * Written starting in 2022 by 20kdc
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

// This is where the fun begins
// Also note we deliberately avoid doing anything that could poison our symbols with libc versioning

extern int puts(const char * text);
extern int printf(const char * fmt, ...);
extern int strcmp(const char * a, const char * b);

// -- Fake GTK 1.2 Type System --
#define GTK12_TYPE_OBJECT 21
// this is silly but it's just a workaround
// it kind of amazes me, GTK 1.2 and they already had this convoluted type system
// luckily, the only part of this ever actually accessed by the game is classfield (presumably via GTK_OBJECT())
typedef struct {
	// this has a "fundamental type", 21 means object
	int type;
} gtk12_class_t;

typedef struct {
	void * classfield;
} gtk12_object_t;

static gtk12_class_t the_one_class = {GTK12_TYPE_OBJECT};

// -- Fake GTK 1.2 Objects --

static gtk12_object_t the_one_object = {&the_one_class};
static gtk12_object_t the_continue_object = {&the_one_class};

// moving swiftly on!

void gtk_init() {
}

void * gtk_window_new() {
	return &the_one_object;
}

int gtk_container_get_type() {
	return GTK12_TYPE_OBJECT;
}

void * gtk_type_check_object_cast() {
	printf("gtk_type_check_object_cast was actually called, meep!\n");
	return (void *) 0;
}

void gtk_widget_destroy() {
}

void gtk_container_set_border_width() {
}

int gtk_window_get_type() {
	return GTK12_TYPE_OBJECT;
}

void gtk_window_set_default_size() {
}

void gtk_window_set_title() {
}

void * gtk_vbox_new() {
	return &the_one_object;
}

void * gtk_hbox_new() {
	return &the_one_object;
}

void * gtk_text_new() {
	return &the_one_object;
}

int gtk_text_get_type() {
	return GTK12_TYPE_OBJECT;
}

void gtk_text_set_editable() {
}

void gtk_text_insert(void * widget, void * idk1, void * idk2, void * idk3, const char * text, int blah) {
	puts(text);
}

void * gtk_button_new_with_label(const char * text) {
	return strcmp(text, "Continue") ? &the_one_object : &the_continue_object;
}

void gtk_container_add() {
}

int gtk_box_get_type() {
	return GTK12_TYPE_OBJECT;
}

void gtk_box_pack_start() {
}

void gtk_signal_connect(void * widget, const char * signal, void (*callback)(void *, void *), void * userdata) {
	// cheeky trick, locate Continue button and press it
	if (widget == &the_continue_object) {
		callback(widget, userdata);
	}
}

void gtk_signal_connect_object(void * widget, const char * signal, void * callback, void * obj) {
	// nope!
}

void gtk_widget_show_all() {
}

void gtk_main() {
	// we just immediately exit
}

