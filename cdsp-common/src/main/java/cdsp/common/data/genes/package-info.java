/*
 * c3ds-projects - Assorted compatibility fixes & useful tidbits
 * Written starting in 2022 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

/**
 * Whereas the genetics package focuses on the 'raw view' of Creatures genetics, the genes package focuses on the 'engineering view'.
 * The genetics package is very good at operations which need to concern themselves with transcription issues and so forth.
 * But as a result, it is worse at operations which require a clear and consistent sequence.
 * The genes package exists to fill this gap in the API.
 */
package cdsp.common.data.genes;
