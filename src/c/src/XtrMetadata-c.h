/*
* Copyright (c) 2005,2006,2007 The Regents of the University of California.
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#ifndef XTRMETADATA_C_H 
#define XTRMETADATA_C_H 

#define MAX_OPTION_LENGTH 255

enum xtr_status {
   XTR_SUCCESS,		/* Success! */
   XTR_NOMEM,		/* Out of memory error */
   XTR_PARAM,		/* Invalid parameter */
   XTR_BUFSZ		/* A supplied buffer was too large or small */
};

/**************** Data Structure Definitions ******************/
/** Metadata Data Structure */
typedef struct xtr_md_st xtr_md_t;

/** Task ID (part of the metadata) */
typedef struct xtr_task_st xtr_task_t;

/** Operation ID (part of the metadata) */
typedef struct xtr_opid_st xtr_opid_t;

/** Options Block (part of the metadata)
    May contains multiple individual options */
typedef struct xtr_options_st xtr_options_t;

/** Individual Option (part of the metadata) */
typedef struct xtr_option_st xtr_option_t;

/**
 * Allocate and initialize a new task id with given size and initial contents
 *
 * Parameters:
 *    taskid: Pointer to the pointer that will be initialized
 *    size:  length of task id, in bytes
 *    data:  initial contents of the task id, must be at least 'size' byte
 *           long
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_NOMEM if out of memory
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status alloc_taskid(xtr_task_t ** taskid, int size, uint8_t * data);

/**
 * Initialize a new task id with given size and initial contents using
 * user-provided memory
 *
 * Parameters:
 *    taskid: Pointer to the memory that will be initialized
 *    size:  length of task id, in bytes
 *    data:  initial contents of the task id, must be at least 'size' byte
 *           long
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status init_taskid(void * taskid, int size, uint8_t * data);

/**
 * Allocate and initialize a new task id with given size and random contents
 *
 * Parameters:
 *    taskid: Pointer to the pointer that will be initialized
 *    size:  length of task id, in bytes
 *
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_NOMEM if out of memory
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status alloc_rand_taskid(xtr_task_t ** taskid, int size);

/**
 * Initialize a new task id with given size and random contents using
 * user-provided memory
 *
 * Parameters:
 *    taskid: Pointer to the memory that will be initialized
 *    size:  length of task id, in bytes
 *
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status init_rand_taskid(void * taskid, int size);

/**
 * Allocate and initialize a new operation id with given size and initial
 * contents
 *
 * Parameters:
 *    opid: Pointer to the pointer that will be initialized
 *    size:  length of operation id, in bytes
 *    data:  initial contents of the operation id, must be at least 'size' byte
 *           long
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_NOMEM if out of memory
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status alloc_opid(xtr_opid_t ** opid, int size, uint8_t * data);

/**
 * Initialize a new operation id with given size and initial contents using
 * user-provided memory
 *
 * Parameters:
 *    opid:  Pointer to the memory that will be initialized
 *    size:  length of operation id, in bytes
 *    data:  initial contents of the operation id, must be at least
 *           'size' byte long
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status init_opid(void * opid, int size, uint8_t * data);

/**
 * Allocate and initialize a new operation id with given size and random
 * contents
 *
 * Parameters:
 *    opid:  Pointer to the pointer that will be initialized
 *    size:  length of operation id, in bytes
 *
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_NOMEM if out of memory
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status alloc_rand_opid(xtr_opid_t ** opid, int size);

/**
 * Initialize a new operation id with given size and random contents using
 * user-provided memory
 *
 * Parameters:
 *    opid: Pointer to the memory that will be initialized
 *    size:  length of operation id, in bytes
 *
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid
 */
xtr_status init_rand_opid(void * opid, int size);

/**************** Accessor/Setter Routines ******************/

/**
 * Gets the contents of a task id
 *
 * Parameters:
 *    taskid: the task id to get the contents of
 *    buf: the buffer to copy the contents into
 *    len:  length of 'buf', in bytes.
 *
 * Returns:
 *    len: the number of bytes copied into 'buf'
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid
 *    XTR_BUFSZ if 'len' is not big enough to hold the contents
 */
xtr_status get_taskid(xtr_task_t * taskid, void * buf, int * len);

/**
 * Sets the contents of an already allocated task id
 *
 * Parameters:
 *    taskid: the task id to update
 *    contents: the data used to update the task id
 *    len:  number of bytes of contents to copy into the task id.  Must
 *          be the same as the length of the task id (i.e., you cannot change
 *          the length of the task id with this function)
 *
 * Returns:
 *    XTR_SUCCESS on success
 *    XTR_PARAM if a parameter is invalid or 'len' is not the same as the
 *        length of the taskid
 */
xtr_status set_taskid(xtr_task_t * taskid, uint8_t * contents, int len);

/**
 * Task ID -- Test for equality 
 *
 * Parameters:
 *    taskid1: the first task id to test
 *    taskid2: the second task id to test
 *
 * Returns:
 *    result of equality test
 *    
 */
bool taskid_eq(xtr_task_t * taskid1, xtr_task_t * taskid2);

#endif
