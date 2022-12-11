#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

void dumpLine(unsigned char b) {
	int x;
	printf("    ");
	for (x = 4; x >= 0; --x) {
		if ((b & (1 << x)) != 0) {
			printf("@");
		} else {
			printf(".");
		}
	}
	printf("\n");
}

int main(int argc, char **argv) {
	int x, y;
	int base = 0;
	int num = 128;
	int bpc = 16;
	int kil = 0;
	int inv = 0;
	int fd = -1;
	struct stat stb;
	unsigned char *buf;

	extern int optind;
	extern char *optarg;

	while ((x = getopt(argc, argv, "b:c:in:k:")) != EOF) {
		switch(x) {
		case 'b':
			base = strtoul(optarg, NULL, 0);
			break;
		case 'i':
			++inv;
			break;
		case 'n':
			num = strtoul(optarg, NULL, 0);
			break;
		case 'c':
			bpc = strtoul(optarg, NULL, 0);
			break;
		case 'k':
			kil = strtoul(optarg, NULL, 0);
			break;
		}
	}
	x = optind;

	if (x < argc) {
		fd = open(argv[x], O_RDONLY);
		if (fd >= 0) {
			fstat(fd, &stb);
		}
	}
	if (fd < 0) {
		fprintf(stderr, "Usage: %s [-b base][-n num][-i][-c bpc] <rom>\n", argv[0]);
		exit(1);
	}
	buf = malloc(stb.st_size);
	read(fd, buf, stb.st_size);
	close(fd);

	for (x = 0; x < num; ++x) {
		if (x < ' ' || x > '~') {
			printf("\nCHAR 0x%02x\n", x);
		} else {
			printf("\nCHAR '%c'\n", x);
		}
		int a = x * bpc + base;
		for (y = 0; y < bpc; ++y) {
			if (y < kil) continue;
			int b = buf[a + y] & 0x0ff;
			if (inv) {
				b = b ^ 0x0ff;
			}
			dumpLine(b);
		}
	}
}
