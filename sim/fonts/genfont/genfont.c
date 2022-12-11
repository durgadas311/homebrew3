#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <pwd.h>
#include <time.h>
#include <string.h>
#include <unistd.h>

//double seg_t = 126.4;
//double seg_h = 764.72;
//double seg_w = 673.08;
double skew = 0.0; // 2.0 / 23.0;
//double gap = 56.88;
//double org_x = 337.2;
//double org_y = 167.48;
double wid = 5.0 * 2.0 * 128.0 + 384;
double hei = 256.0;
double dotx = 128.0;
/*
 * LED array layout:
 *
 *    @ @ @ @ <--
 *    @ @ @ @ <--
 *    @ @ @ @ <--
 *    @ @ @ @ <--
 *    @ @ @ @ <--
 *    @ @ @ @ <--
 *    @ @ @ @ <-- 0
 *    ^ ^ ^ ^-
 *    | | +--- 
 *    | +----- 256
 *    +------- 0
 */

void do_dot(double x, double y) {
	printf("%f %f m 1\n", x + (y * skew), y);
	y += dotx;
	printf(" %f %f l 1\n", x + (y * skew), y);
	x += dotx;
	printf(" %f %f l 1\n", x + (y * skew), y);
	y -= dotx;
	printf(" %f %f l 1\n", x + (y * skew), y);
	x -= dotx;
	printf(" %f %f l 1\n", x + (y * skew), y);
}

void do_row(int b, double y) {
	int x;
	for (x = 0; x < 5; ++x) {
		if (b & 1) {
			do_dot((5.0 - x) * 2.0 * dotx, y);
		}
		b >>= 1;
	}
}

void do_char(int c, int fc) {
	int r;
	int b;
	static int cn = 0;

	printf("StartChar: uni%04X\n", fc);
	printf("Encoding: %d %d %d\n", fc, fc, cn++); // what is 3rd number?
	if (c < 0) {
		printf("Width: %d\n", -c);
	} else {
		printf("Width: %f\n", wid);
	}
	printf("VWidth: 0\n");
	printf("Flags: HW\n");
	printf("LayerCount: 2\n");
	printf("Fore\n");
	if (c >= 0) {
		printf("SplineSet\n");
		b = c & 0x1f;
		do_row(b, 0.0);
		printf("EndSplineSet\n");
	}
	printf("EndChar\n\n");
}

static void preamble(int ascent, int descent, char *name, char *arg) {
	struct passwd *pw = getpwuid(getuid());
	time_t t = time(NULL);
	struct tm *tm = localtime(&t);
	char *user = pw->pw_gecos;
	if (*user == 0 || *user == ' ') {
		user = pw->pw_name;
	} else {
		int n = strlen(user);
		while (n > 0 && user[n - 1] == ',') {
			user[--n] = 0;
		}
	}
	printf(	"SplineFontDB: 3.0\n"
		"FontName: %s\n"
		"FullName: %s\n"
		"FamilyName: %s\n"
		"Weight: Medium\n"
		"Copyright: Created by %s with genfont %s\n"
		"UComments: \"%04d-%d-%d: Created.\" \n"
		"Version: 001.000\n"
		"ItalicAngle: 0\n"
		"UnderlinePosition: 0\n"
		"UnderlineWidth: 0\n"
		"Ascent: %d\n"
		"Descent: %d\n"
		"LayerCount: 2\n"
		"Layer: 0 0 \"Back\"  1\n"
		"Layer: 1 0 \"Fore\"  0\n"
		"XUID: [1021 590 %ld 919824]\n"
		"FSType: 0\n"
		"OS2Version: 0\n"
		"OS2_WeightWidthSlopeOnly: 0\n"
		"OS2_UseTypoMetrics: 1\n"
		"CreationTime: %ld\n"
		"ModificationTime: %ld\n"
		"OS2TypoAscent: 0\n"
		"OS2TypoAOffset: 1\n"
		"OS2TypoDescent: 0\n"
		"OS2TypoDOffset: 1\n"
		"OS2TypoLinegap: 90\n"
		"OS2WinAscent: 0\n"
		"OS2WinAOffset: 1\n"
		"OS2WinDescent: 0\n"
		"OS2WinDOffset: 1\n"
		"HheadAscent: 0\n"
		"HheadAOffset: 1\n"
		"HheadDescent: 0\n"
		"HheadDOffset: 1\n"
		"OS2Vendor: 'PfEd'\n"
		"DEI: 91125\n"
		"Encoding: Custom\n"
		"UnicodeInterp: none\n"
		"NameList: Adobe Glyph List\n"
		"DisplaySize: -24\n"
		"AntiAlias: 1\n"
		"FitToEm: 1\n"
		"WinInfo: 16 16 15\n",
		name, name, name,
		user, arg,
		tm->tm_year + 1900, tm->tm_mon + 1, tm->tm_mday,
		ascent, descent,
		t, t, t);
}

static void genchars() {
#if 1
	int c;
# if 0
	// Generate spacing chars:
	for (c = 1; c < 10; ++c) {
		int w = ((wid * c / 10.0) + 0.5);
		do_char(-w, c);
	}
#endif
	// Generate display chars:
	for (c = 0; c < 32; ++c) {
		int fc = c + 0x20;
		do_char(c, fc);
	}
#else
	do_char(0b01111111, 32);
#endif
}

int main(int argc, char **argv) {
	int c;
	int x, y;
	char *name = "Untitled";

	extern int optind;
	extern char *optarg;

	while ((x = getopt(argc, argv, "N:")) != EOF) {
		switch(x) {
		case 'N':
			name = optarg;
			break;
		}
	}
	x = optind;

	if (0) {
		fprintf(stderr,
			"Usage: %s [options]\n"
			"Options:\n"
			"         -N name  Name to use in output SFD data\n"
			, argv[0]);
		exit(1);
	}

	preamble(192, 0, name, argv[x]);

	int max = 0x100 + 32; // assume at least 0x100-0x11f
	int cnt = 16;
	printf("\nBeginChars: %d %d\n\n", max, cnt);

	genchars();
	printf("\nEndChars\n"
		"EndSplineFont\n");

	return 0;
}
